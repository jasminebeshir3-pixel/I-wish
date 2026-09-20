package org.example;


import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class TestClient {

    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 5000);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to server. Type a command (or 'exit' to quit):");

            String line;
            while (true) {
                System.out.print("> ");
                line = scanner.nextLine();
                if (line.equalsIgnoreCase("exit")) break;

                out.println(line);
                String response = in.readLine();
                System.out.println("Server response: " + response);
            }

        } catch (IOException e) {
            System.err.println("Could not connect to server: " + e.getMessage());
        }
    }
}
