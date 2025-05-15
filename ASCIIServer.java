import java.io.*;
import java.net.*;

public class ASCIIServer {
    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        try {
            // 서버 소켓 생성
            serverSocket = new ServerSocket(12345);
            System.out.println("Server started on port 12345...");

            while (true) {
                Socket clientSocket = null;
                BufferedReader in = null;
                PrintWriter out = null;
                try {
                    // 클라이언트 연결 대기
                    clientSocket = serverSocket.accept();
                    System.out.println("Client connected: " + clientSocket.getInetAddress());

                    // 입력 스트림
                    in = new BufferedReader(
                            new InputStreamReader(clientSocket.getInputStream()));
                    // 출력 스트림
                    out = new PrintWriter(clientSocket.getOutputStream(), true);

                    // 클라이언트로부터 문자열 수신
                    String input = in.readLine();
                    if (input == null) {
                        continue;
                    }

                    // "exit" 입력 시 서버 종료
                    if ("exit".equalsIgnoreCase(input)) {
                        System.out.println("Received exit command. Shutting down server...");
                        break;
                    }

                    // 문자열을 아스키 코드로 변환
                    StringBuilder asciiCodes = new StringBuilder();
                    asciiCodes.append("ASCII codes: [");
                    for (int i = 0; i < input.length(); i++) {
                        asciiCodes.append((int) input.charAt(i));
                        if (i < input.length() - 1) asciiCodes.append(", ");
                    }
                    asciiCodes.append("]");

                    // 결과 전송
                    out.println(asciiCodes.toString());

                } catch (IOException e) {
                    System.err.println("Error handling client: " + e.getMessage());
                } finally {
                    // 클라이언트 리소스 정리
                    try {
                        if (out != null) out.close();
                        if (in != null) in.close();
                        if (clientSocket != null) clientSocket.close();
                    } catch (IOException e) {
                        System.err.println("Error closing client resources: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            // 서버 소켓 정리
            try {
                if (serverSocket != null && !serverSocket.isClosed()) {
                    serverSocket.close();
                    System.out.println("Server socket closed.");
                }
            } catch (IOException e) {
                System.err.println("Error closing server socket: " + e.getMessage());
            }
        }
    }
}