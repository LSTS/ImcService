package pt.lsts.imcservice;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import pt.lsts.imc4j.annotations.Consume;
import pt.lsts.imc4j.annotations.Periodic;
import pt.lsts.imc4j.msg.*;
import pt.lsts.imc4j.net.TcpClient;
import pt.lsts.imc4j.util.FormatConversion;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class ImcService {

    private CopyOnWriteArrayList<NetSocket> jsonClients = new CopyOnWriteArrayList<>();
    private TcpClient dune = new TcpClient();

    private double depth = 0;

    public ImcService(int dunePort, int serverPort) {
        try {
            initServer(serverPort);
            connectToDune(dunePort);
        }
        catch (Exception e) {
            Logger.getLogger(getClass().getSimpleName()).warning("Could not start server: "+e.getMessage());
        }
    }

    @Periodic(120_000)
    void startPlan() {
        TextMessage msg = new TextMessage();
        msg.origin = "ImcServer";
        msg.text = "dive";
        try {
            dune.send(msg);
            Logger.getLogger(getClass().getSimpleName()).info("Requested vehicle to dive");
        }
        catch (Exception e) {
            Logger.getLogger(getClass().getSimpleName()).warning("Could not request plan: "+e.getMessage());
        }
    }

    @Consume
    void onMsg(Message m) {
        String data = m.toString()+"\n";

        // update simulated depth
        if (m.mgid() == SimulatedState.ID_STATIC) {
            depth = ((SimulatedState) m).z;
            jsonClients.forEach(client -> client.write(data));
        }
        else if (depth <= 0.2)
            jsonClients.forEach(client -> client.write(data));
    }

    private void connectToDune(int port) throws Exception {
        dune.connect("localhost", port);
        dune.register(this);
    }

    void initServer(int tcpPort) throws Exception {
        NetServer server = Vertx.vertx().createNetServer();
        server.connectHandler(this::clientConnected);

        server.listen(tcpPort, "localhost", res -> {
            if (res.succeeded()) {
                Logger.getLogger(getClass().getSimpleName()).info("TCP server listening on port "+server.actualPort());
            }
            else {
                Logger.getLogger(ImcService.class.getSimpleName()).warning("Failed to start IMCServer: "+res.cause().getMessage());
            }
        });
    }

    // TCP server initialization
    void clientConnected(NetSocket socket) {
        jsonClients.add(socket);
        Logger.getLogger(getClass().getSimpleName()).info("Client connected: "+socket.remoteAddress());

        socket.handler(data -> {
            byte buffer[] = data.getBytes();
            try {
                 Message m = FormatConversion.fromJson(new String(buffer));
                 dune.send(m);
            }
            catch (Exception e) {
                Logger.getLogger(getClass().getSimpleName()).warning("Message not understood: "+e.getMessage());
            }
        });
        socket.closeHandler( s -> {
            jsonClients.remove(socket);
            Logger.getLogger(getClass().getSimpleName()).info("Client disconnected: "+socket.remoteAddress());
        });
    }


    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java -jar ImcServer.jar <dune-port> <port-to-listen>");
            System.exit(1);
        }

        try {
            ImcService service = new ImcService(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        }
        catch (Exception e) {
            Logger.getLogger(ImcService.class.getSimpleName()).warning("Failed to start IMCServer: "+e.getMessage());
            System.exit(1);
        }
    }
}