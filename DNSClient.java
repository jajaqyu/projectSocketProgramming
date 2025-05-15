import java.io.*;
import java.net.*;

public class DNSClient {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("192.168.0.1", 44444);
            //config: ipv4)192.168.0.8  기본 게이트웨이)192.168.0.1
            //curl ifconfig.me)182.215.155.16
            //(서버 컴퓨터에서)
            //윈도우 방화벽 설정에서 포트번호 44444에 접근 허용
            //게이트웨이를 브라우저에서 접속해 라우터 포워딩 설정
            //그 후 다른 네트워크 상에 컴퓨터로 공인 IP를 입력
            //외부에서
            System.out.println("Connected to DNS Server");

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                System.out.println("Enter command (e.g., N:domain.name, R:ip.address, W:domain,ip, or 'exit' to quit):");
                String command = console.readLine();
                if ("exit".equalsIgnoreCase(command)) {
                    break;
                }

                out.println(command);
                String response = in.readLine();
                System.out.println("Server response: " + response);
            }

            out.close();
            in.close();
            console.close();
            socket.close();
            System.out.println("Client disconnected.");

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}