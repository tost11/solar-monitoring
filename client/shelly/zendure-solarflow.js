// Konfiguration
let ZENDURE_HOST = "SOLARFLOW_IP_OR_HOSTNAME"; // Nur für GET der Properties
let CLIENT_TOKEN = "SYSTEM_TOKEN"; // Muss am Anfang definiert werden
let DATA_DURATION = 30; // Zeitabstand in Sekunden für die Daten
let DEBUG = false;
let SYSTEM_ID = "SYSTEM_ID";
let ONLINE_URL = "https://solar.pihost.org";

function log(message, debug) {
    if (!debug || DEBUG) {
        let prefix = "[" + Shelly.getUptimeMs() + " Zendure Parser]: ";
        let fullMessage = prefix + message;
        print(fullMessage);
    }
}

// Parsing Funktion mit sauberem Entfernen von Null- und 0-Werten
function parseZendureData(resp) {
    let p = resp.properties || {};
    let pd = resp.packData || [];
    let now = Date.now();

    function addIfValid(obj, key, val) {
        if (val !== undefined && val !== null && val !== 0) {
            obj[key] = val;
        }
    }

    let inputsDC = [];
    for (let i = 1; i <= 4; i++) {
        let key = "solarPower" + i;
        if (typeof p[key] === "number" && p[key] !== 0) {
            inputsDC.push({
                id: i,
                watt: p[key]
            });
        }
    }

    let inputsAC = [];
    let outputsAC = [];
    let batteries = [];
    //TODO pars packData

    let device = {
        id: 1
    };

    let packPower = 0;
    let packPowerOk = false;
    if (typeof p.packInputPower === "number" && p.packInputPower > 0) {
        packPower -= p.packInputPower;
        packPowerOk = true;
    }
    if (typeof p.outputPackPower === "number" && p.outputPackPower > 0) {
        packPower = p.outputPackPower;
        packPowerOk = true;
    }
    if (packPowerOk){
        device.batteryWatt = packPower;
    }
    if (typeof p.solarInputPower === "number" && p.solarInputPower > 0) {
        device.inputWattDC = p.solarInputPower;
    }
    if (typeof p.outputHomePower === "number" && p.outputHomePower > 0) {
        device.outputWattAC = p.outputHomePower;
    }
    if (typeof p.gridInputPower === "number" && p.gridInputPower > 0) {
        device.inputWattAC = p.gridInputPower;
    }
    if (typeof p.BatVolt === "number" && p.BatVolt > 0) {
        device.batteryVoltage = p.BatVolt / 100.0;
    }
    if (typeof p.electricLevel === "number" && p.electricLevel !== 0) {
        device.batteryPercentage = p.electricLevel;
    }
    if (typeof p.hyperTmp === "number" && p.hyperTmp !== 0) {
        device.temperature = p.hyperTmp / 100.0;
    }

    if (inputsDC.length) device.inputsDC = inputsDC;
    if (inputsAC.length) device.inputsAC = inputsAC;
    if (batteries.length) device.batteries = batteries;
    if (outputsAC.length) device.outputsAC = outputsAC;

    return {
        duration: DATA_DURATION,
        timestamp: now,
        devices: [device]
    };
}

function runScript() {
    Shelly.call("HTTP.GET", {url: "http://" + ZENDURE_HOST + "/properties/report"},function(result, err_code, err_msg) {
        if (err_code !== 0) {
            log("Fehler beim GET von Zendure: " + err_code + " " + err_msg, false);
            return;
        }

        //log("headers: " + result.headers,true);
        //log("code: " + result.code,true);
        //TODO find out why this is not working (to long i suppose)
        //log("body: " + result.body,true);
        let response = JSON.parse(result.body);
        if (!response.properties) {
            log("Keine Properties in Zendure Antwort", false);
            return;
        }

        let payload = parseZendureData(response);
        let toSend = JSON.stringify(payload)

        log("sendData: "+toSend,true);

        Shelly.call("HTTP.Request", {
            method: "POST",
            url: ONLINE_URL+"/api/solar/data?systemId="+SYSTEM_ID,
            headers: {
                "Content-Type": "application/json",
                "clientToken": CLIENT_TOKEN
            },
            body: toSend
        }, function(postResult, postErrCode, postErrMsg) {
            if (postErrCode === 0) {
                log("POST an " + ONLINE_URL + " erfolgreich", false);
            } else {
                log("Fehler beim POST: " + postErrCode + " " + postErrMsg, false);
            }
        });
    });
}

// Timer alle DATA_DURATION Sekunden für Datenerfassung & Übertragung
Timer.set(DATA_DURATION * 1000, true, runScript, null);