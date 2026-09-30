import { networkInterfaces, hostname, platform, arch, cpus } from "os";
import { createHash } from "crypto";
import { MachineInfo } from "./types.js";

export class Machine {
    mac(): string {
        const interfaces = networkInterfaces();
        for (const name of Object.keys(interfaces)) {
            const list = interfaces[name];
            if (list !== undefined && list !== null) {
                for (const item of list) {
                    if (!item.internal && item.mac !== "00:00:00:00:00:00") {
                        return item.mac;
                    }
                }
            }
        }
        return "00:00:00:00:00:00";
    }

    hwid(): string {
        const cpuList = cpus();
        const cpuModel = cpuList.length > 0 ? cpuList[0].model : "generic";
        return createHash("sha256")
            .update(platform())
            .update(arch())
            .update(hostname())
            .update(cpuModel)
            .update(this.mac())
            .digest("hex");
    }

    get(): MachineInfo {
        return {
            hwid: this.hwid(),
            platform: platform(),
            arch: arch(),
            hostname: hostname(),
            mac: this.mac()
        };
    }
}
