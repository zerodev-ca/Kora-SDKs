import { networkInterfaces, hostname, platform, arch, cpus } from "os";
import { createHash } from "crypto";

export class Machine {
    mac(): string {
        for (const list of Object.values(networkInterfaces())) {
            const found = (list !== undefined ? list : []).find(item => !item.internal && item.mac !== "00:00:00:00:00:00");
            if (found !== undefined) {
                return found.mac;
            }
        }
        return "00:00:00:00:00:00";
    }

    hwid(): string {
        const list = cpus();
        return createHash("sha256").update(platform()).update(arch()).update(hostname()).update(list.length > 0 ? list[0].model : "generic").update(this.mac()).digest("hex");
    }

    name(): string {
        return hostname();
    }

    platform(): string {
        return `${platform()}-${arch()}`;
    }
}
