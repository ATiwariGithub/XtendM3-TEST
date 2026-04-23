/****************************************************************************************
 Extension Name: GetContactPerson
 Type : ExtendM3Transaction
 Script Author: Arun Tiwari
 Date: 2022-02-14

 Description:
       Fetch the default contact person from CRS618 based on customer OR customer order number

 Revision History:
 Name                    Date             Version          Description of Changes
 Arun Tiwari            2022-02-14          1.0              Initial Version
 Arun Tiwari            2026-04-20          2.0              Replaced GetHead to GetOrderHead
******************************************************************************************/
public class GetContactPer extends ExtendM3Transaction {
  private final MIAPI mi
  private final DatabaseAPI database
  private final ProgramAPI program
  private final MICallerAPI miCaller

  public GetContactPer(MIAPI mi, DatabaseAPI database, ProgramAPI program, MICallerAPI miCaller) {
    this.mi = mi
    this.database = database
    this.program = program
    this.miCaller = miCaller
  }

  public void main() {
    int inCono = mi.in.get("CONO")
    String inOrno = mi.inData.get("ORNO")?.trim() ?: ""
    String inCuno = mi.inData.get("CUNO")?.trim() ?: ""

    if (inCuno.isEmpty()) {
      Closure<?> callbackCuno = { Map<String, String> response ->
        if (response.CUNO != null) inCuno = response.CUNO
      }
      miCaller.call("OIS100MI", "GetOrderHead", ["ORNO": inOrno], callbackCuno)
    }

    processContactPerson(inCono, inCuno)
  }

  /**
   * Retrieve contact person from CCUCON, validate against CUGEX1 custom field,
   * and write TX40, EMAL, CNPE to MI output.
   */
  private void processContactPerson(int inCono, String inCuno) {
    boolean custTrue = true
    boolean ccuconValue = true
    String tx40G = null, emalG = null, cnpeG = null

    DBAction ccuconQuery = database.table("CCUCON").index("10").selection("CCTX40", "CCEMAL", "CCCNPE").build()
    DBContainer ccuconContainer = ccuconQuery.getContainer()
    ccuconContainer.set("CCCONO", inCono)
    ccuconContainer.set("CCERTP", 1)
    ccuconContainer.set("CCEMRE", inCuno)

    DBAction cugex1Query = database.table("CUGEX1").index("00").selection("F1CHB1").build()
    DBContainer cugex1Container = cugex1Query.getContainer()

    Closure<?> processRecord = { DBContainer rec ->
      String cnpe = rec.getString("CCCNPE").trim()
      String tx40 = rec.getString("CCTX40").trim()
      String emal = rec.getString("CCEMAL").trim()

      cugex1Container.set("F1CONO", inCono)
      cugex1Container.set("F1FILE", "CCUCON")
      cugex1Container.set("F1PK01", cnpe)

      Closure<?> processCugex1Record = { DBContainer rec1 ->
        if (rec1.get("F1CHB1").toString().equals("1") && custTrue) {
          custTrue = false
          tx40G = tx40; emalG = emal; cnpeG = cnpe
        }
      }
      cugex1Query.readAll(cugex1Container, 3, 1, processCugex1Record)

      if (custTrue && ccuconValue) {
        ccuconValue = false
        tx40G = tx40; emalG = emal; cnpeG = cnpe
      }
    }

    if (!ccuconQuery.readAll(ccuconContainer, 3, 1, processRecord)) {
      mi.error("The record does not exist in CCUCON table")
      return
    }

    mi.outData.put("TX40", tx40G)
    mi.outData.put("EMAL", emalG)
    mi.outData.put("CNPE", cnpeG)
    mi.write()
  }
}
