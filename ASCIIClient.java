import java.io.*;
import java.net.*;

public class ASCIIClient {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 12345);
            System.out.println("Connected to server");

            // 출력 스트림
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            // 입력 스트림
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            // 사용자 입력
            BufferedReader console = new BufferedReader(
                    new InputStreamReader(System.in));

            // 문자열 입력
            System.out.print("Enter a string: ");
            String message = console.readLine();

            // 서버로 전송
            out.println(message);

            // 서버로부터 응답 수신
            String response = in.readLine();
            System.out.println(response);

            // 소켓 닫기
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}