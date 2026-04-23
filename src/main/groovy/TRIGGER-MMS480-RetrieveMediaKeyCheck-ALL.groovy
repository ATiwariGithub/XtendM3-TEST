/**
 *  Business Engine Extension
 */
 /****************************************************************************************
 Extension Name: EXT480
 Type : ExtendM3Trigger
 Script Author: Arun Tiwari
 Date: 2022-05-26
  
 Description:
       set the consignee based on order division
          
 Revision History:
 Name                    Date             Version          Description of Changes
 Arun Tiwari             2022-05-26         1.0              Initial Version
 Arun Tiwari             2026-04-20         2.0              Replaced GetHead to GetOrderHead
 ******************************************************************************************/
 
public class RetrieveMediaKeyCheck extends ExtendM3Trigger {
  private final MethodAPI method
  private final DatabaseAPI database
  private final ProgramAPI program
  private final MICallerAPI miCaller

  public RetrieveMediaKeyCheck(MethodAPI method, DatabaseAPI database, ProgramAPI program, MICallerAPI miCaller) {
    this.method = method
    this.database = database
    this.program = program
    this.miCaller = miCaller
  }

  public void main() {
    long dlix = (Long) method.getArgument(1)
    Media media = retrieveMediaKeys(dlix)
    if(media != null)
      method.setReturnValue(media)
  }

  /**
   * Retrieve and set CONA as media based on CRS949 partner media lookup,
   * else fall back to Invoice receipt (INRC).
   */
  public Media retrieveMediaKeys(long dlix) {
    int cono = (Integer) program.getLDAZD().CONO
    boolean flag = true
    String coaf = ""
    String cona = ""
    String whlo = ""
    String inrc = ""
    String divi = ""

    //MHDISH
    DBAction mhdishQuery = database.table("MHDISH").index("00").selection("OQCOAF", "OQCONA", "OQWHLO").build()
    DBContainer mhdishContainer = mhdishQuery.getContainer()
    mhdishContainer.set("OQCONO", cono)
    mhdishContainer.set("OQDLIX", dlix)
    mhdishContainer.set("OQINOU", 1)

    Closure<?> processCoafRecord = { DBContainer recordCoaf ->
      coaf = recordCoaf.getString("OQCOAF").trim()
      cona = recordCoaf.getString("OQCONA").trim()
      whlo = recordCoaf.getString("OQWHLO").trim()
    }
    mhdishQuery.readAll(mhdishContainer, 3, 1000, processCoafRecord)

    //MHDISL
    DBAction mhdislQuery = database.table("MHDISL").index("00").selection("URRIDN").build()
    DBContainer mhdislContainer = mhdislQuery.getContainer()
    mhdislContainer.set("URCONO", cono)
    mhdislContainer.set("URDLIX", dlix)
    mhdislContainer.set("URRORC", 3)

    Closure<?> processDlixRecord = { DBContainer recordDlix ->
      String orno = recordDlix.getString("URRIDN").trim()
      if(flag && orno) {
        Map<String, String> paramsOrno = ["ORNO": orno]
        Closure<?> callbackOrno = { Map<String, String> response ->
          if(response.INRC != null) {
            inrc = response.INRC
          }
        }
        miCaller.call("OIS100MI", "GetOrderHead", paramsOrno, callbackOrno)
        flag = false
      }
    }
    mhdislQuery.readAll(mhdislContainer, 3, 1000, processDlixRecord)

    //Get DIVI from WHLO
    if(whlo) {
      Map<String, String> paramsWhlo = ["WHLO": whlo]
      Closure<?> callbackWhlo = { Map<String, String> response ->
        if(response.DIVI != null) {
          divi = response.DIVI
        }
      }
      miCaller.call("MMS005MI", "GetWarehouse", paramsWhlo, callbackWhlo)
    }

    boolean crs949Match = false
    if(cona) {
      Map<String, String> paramsCrs = ["DIVI": divi, "DONR": "900", "DOVA": "00", "PRF1": cona, "MEDC": "MBM", "SEQN": "1"]
      Closure<?> callbackCrs = { Map<String, String> response ->
        if(response.SIID != null) {
          crs949Match = true
        }
      }
      miCaller.call("CRS949MI", "GetPartnerMedia", paramsCrs, callbackCrs)
    }

    if(crs949Match) {
      return new Media(cona, coaf, false)
    }
    return new Media(inrc, coaf, false)
  }
}
