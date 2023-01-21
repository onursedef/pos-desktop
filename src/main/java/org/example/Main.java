package org.example;

import javax.swing.*;
import java.awt.*;
import java.net.URI;

import io.socket.client.IO;
import io.socket.client.Socket;
import org.json.JSONException;
import org.json.JSONObject;

public class Main extends JFrame {

    public Main() {
        setTitle("POS Application");
        setSize(1280, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setLayout(new GridLayout(3, 1));

        JLabel label = new JLabel("Waiting for response");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        add(label);

        URI wsUri = URI.create("http://localhost:3000");
        Socket socket = IO.socket(wsUri);
        socket.connect();
        socket.on(Socket.EVENT_CONNECT, args -> {
            System.out.println("Connected");
        });

        socket.on("message", args -> {
            System.out.println("Message: " + args[0]);
            String message = args[0].toString();
            try {
                JSONObject obj = new JSONObject(message);
                String type = obj.getString("type");

                if (type.equals("BANK_APPROVE_PENDING")) {

                    String productName = obj.getString("name");
                    String productPrice = obj.getString("price");
                    String productCode = obj.getString("code");

                    label.setText("Product: " + productName);
                    JLabel label2 = new JLabel("Price: " + productPrice);
                    label2.setHorizontalAlignment(SwingConstants.CENTER);
                    label2.setVerticalAlignment(SwingConstants.CENTER);
                    add(label2);

                    JPanel buttonContainer = new JPanel();
                    buttonContainer.setLayout(new GridLayout(1, 2));

                    JButton approveButton = new JButton("Approve");
                    approveButton.setBackground(Color.GREEN);

                    Timer timer = new Timer(20000, e -> {
                        label2.setVisible(false);
                        buttonContainer.setVisible(false);
                    });
                    timer.setRepeats(false);

                    approveButton.addActionListener(e -> {
                        // send approve message to server
                        String wsJson = "{\"type\": \"BANK_APPROVED\", \"name\": \"" + productName + "\",\"price\": \"" + productPrice + "\", \"code\": \"" + productCode + "\"}";
                        socket.send(wsJson);

                        timer.start();
                    });
                    buttonContainer.add(approveButton);


                    JButton rejectButton = new JButton("DECLINE");
                    rejectButton.setBackground(Color.RED);
                    rejectButton.addActionListener(e -> {
                        // send reject message to server
                        String wsJson = "{\"type\": \"BANK_DECLINED\", \"name\": \"" + productName + "\",\"price\": \"" + productPrice + "\", \"code\": \"" + productCode + "\"}";
                        socket.send(wsJson);
                        timer.start();
                    });
                    buttonContainer.add(rejectButton);

                    add(buttonContainer);
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });

        socket.on(Socket.EVENT_DISCONNECT, args -> {
            System.out.println("Disconnected");
            String wsJson = "{\"type\": \"BANK_DECLINED\"}";
            socket.send(wsJson);
            socket.close();
        });

        socket.on(Socket.EVENT_CONNECT_ERROR, args -> {
            System.out.println("Connection error");
            System.out.println("Disconnected");
            String wsJson = "{\"type\": \"BANK_DECLINED\"}";
            socket.send(wsJson);
            socket.close();
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Main main = new Main();

            main.setVisible(true);
        });
    }
}