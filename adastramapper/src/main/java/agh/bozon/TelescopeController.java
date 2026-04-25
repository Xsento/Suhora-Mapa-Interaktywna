package agh.bozon;
import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.procimg.Register;

public class TelescopeController {
    private final String ipAddress;
    private final int port;
    private ModbusTCPMaster master;
    private boolean mockMode;

    public TelescopeController(String ipAddress, int port, boolean mockMode) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.mockMode = mockMode;
    }

    public void connect() throws Exception {
        master = new ModbusTCPMaster(ipAddress, port);
        master.connect();
    }

    public void disconnect() {
        if (master != null) {
            master.disconnect();
        }
    }

    public TelescopeStatus getStatus() throws Exception {
        if (mockMode) {
            return getMockStatus();
        }
        if (master == null) connect();

        // Pobieranie 26 rejestrów (Input Registers) od adresu 8192
        Register[] registers = (Register[]) master.readInputRegisters(8192, 26);
        
        TelescopeStatus status = new TelescopeStatus();
        
        // Dekodowanie w dokładnie tej samej kolejności co w skrypcie Pythona (Big Endian, 2 rejestry = 32 bity)
        status.starAz = Math.toDegrees(decodeFloat(registers[0], registers[1]));
        status.starAlt = Math.toDegrees(decodeFloat(registers[2], registers[3]));
        status.domeAzPV = Math.toDegrees(decodeFloat(registers[4], registers[5]));
        status.alarm = decodeInt(registers[6], registers[7]);
        status.domeAz = Math.toDegrees(decodeFloat(registers[8], registers[9]));
        status.currentMargin = decodeFloat(registers[10], registers[11]);
        status.telTemperature = decodeFloat(registers[12], registers[13]);
        status.lha = decodeFloat(registers[14], registers[15]);
        status.trkState = decodeInt(registers[16], registers[17]);
        status.decPV = Math.toDegrees(decodeFloat(registers[18], registers[19]));
        status.telescopePosition = (float) (Math.toDegrees(decodeFloat(registers[20], registers[21])) / 15.0);
        status.ccdStartTimeOut = decodeInt(registers[22], registers[23]);
        status.ccdExpTimeOut = decodeInt(registers[24], registers[25]);

        return status;
    }

    private TelescopeStatus getMockStatus() {
        TelescopeStatus status = new TelescopeStatus();
        long t = System.currentTimeMillis() / 1000;
        // Symulacja poruszającego się teleskopu w trybie testowym
        status.starAz = 180 + 45 * Math.sin(t * 0.1);
        status.starAlt = 45 + 15 * Math.cos(t * 0.1);
        status.domeAz = status.starAz;
        status.trkState = 1; // Tracking włączony
        status.telTemperature = 15.5f + (float)Math.sin(t * 0.05);
        return status;
    }

    private float decodeFloat(Register r1, Register r2) {
        int bits = (r1.getValue() << 16) | (r2.getValue() & 0xFFFF);
        return Float.intBitsToFloat(bits);
    }

    private int decodeInt(Register r1, Register r2) {
        return (r1.getValue() << 16) | (r2.getValue() & 0xFFFF);
    }
}
