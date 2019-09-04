package com.example.test;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import android.text.method.ScrollingMovementMethod;

public class MainActivity extends AppCompatActivity {

    private EditText txt_ip;
    private EditText txt_port;
    private Button btn_connect;
    private Button btn_disconnect;
    private Button btn_action1;
    private Button btn_action2;
    private Button btn_action3;
    private Button btn_send_message;
    private TextView txt_status;
    private TextView txt_receive_message;
    private EditText txt_send_message;
    private Thread m_threadSocket;
    //private Thread m_threadReceive;

    private Socket m_socket;
    private Handler m_handler;

    BufferedReader m_bufferRead;
    OutputStream outputStream;

    String message;
    String ip;
    String port;

    private Runnable r_buildSocket = new Runnable() {
        public void run() {
            ip = txt_ip.getText().toString();
            port = txt_port.getText().toString();
            try {
                m_socket = new Socket(ip, Integer.parseInt(port));
                outputStream = m_socket.getOutputStream();
                if (m_socket.isConnected()) {
                    m_handler.post(new Runnable() {
                        @Override
                        public void run() {
                            btn_connect.setEnabled(false);
                            btn_disconnect.setEnabled(true);
                            btn_send_message.setEnabled(true);
                            btn_action1.setEnabled(true);
                            btn_action2.setEnabled(true);
                            btn_action3.setEnabled(true);
                            txt_status.setTextColor(getResources().getColor(R.color.connect_color));
                            txt_status.setText(R.string.status_con);
                        }
                    });
                    m_bufferRead = new BufferedReader(new InputStreamReader(m_socket.getInputStream()));

                    m_handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (txt_receive_message.getText().toString().equals("No Message"))
                                txt_receive_message.setText("");
                            txt_receive_message.append("Did connect to " + ip + ":" + port + "\n");
                            int offset = txt_receive_message.getLineCount() * txt_receive_message.getLineHeight();
                            if (offset > txt_receive_message.getHeight()) {
                                txt_receive_message.scrollTo(0, offset - txt_receive_message.getHeight());
                            }
                        }
                    });

                    new Thread(r_receiveMessages).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
    private Runnable r_receiveMessages = new Runnable() {
        @Override
        public void run() {
            while (true) {
                try {
                    final String response = m_bufferRead.readLine();
                    if (response != null) {
                        m_handler.post(new Runnable() {
                            @Override
                            public void run() {
                                txt_receive_message.append("Received data from server: " + response + "\n");
                                int offset = txt_receive_message.getLineCount() * txt_receive_message.getLineHeight();
                                if(offset > txt_receive_message.getHeight()){
                                    txt_receive_message.scrollTo(0,offset - txt_receive_message.getHeight());
                                }
                            }
                        });
                    } else {
                        m_handler.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    outputStream.close();
                                    m_bufferRead.close();
                                    m_socket.close();
                                    btn_connect.setEnabled(true);
                                    btn_disconnect.setEnabled(false);
                                    btn_send_message.setEnabled(false);
                                    btn_action1.setEnabled(false);
                                    btn_action2.setEnabled(false);
                                    btn_action3.setEnabled(false);
                                    txt_status.setTextColor(getResources().getColor(R.color.disconnect_color));
                                    txt_status.setText(R.string.status_dis);
                                    txt_receive_message.append("Connection has stopped.\n");
                                    int offset = txt_receive_message.getLineCount() * txt_receive_message.getLineHeight();
                                    if(offset > txt_receive_message.getHeight()){
                                        txt_receive_message.scrollTo(0,offset - txt_receive_message.getHeight());
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };
    private Runnable r_sendMessages = new Runnable() {
        @Override
        public void run() {
            if(outputStream == null)
                return;
            try {
                //message = txt_send_message.getText().toString();
                outputStream.write((message + "\n").getBytes("utf-8"));
                outputStream.flush();
                m_handler.post(new Runnable() {
                    @Override
                    public void run() {
                        txt_receive_message.append("Sending message to server: " + message + "\n");
                        txt_send_message.setText("");
                        int offset = txt_receive_message.getLineCount() * txt_receive_message.getLineHeight();
                        if(offset > txt_receive_message.getHeight()){
                            txt_receive_message.scrollTo(0,offset - txt_receive_message.getHeight());
                        }
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txt_ip = findViewById(R.id.txt_server_ip);
        txt_port = findViewById(R.id.txt_port);
        btn_connect = findViewById(R.id.btn_connect);
        btn_disconnect = findViewById(R.id.btn_disconnect);
        btn_send_message = findViewById(R.id.btn_send);
        btn_action1 = findViewById(R.id.btn_action1);
        btn_action2 = findViewById(R.id.btn_action2);
        btn_action3 = findViewById(R.id.btn_action3);
        txt_status = findViewById(R.id.label_status);
        txt_receive_message = findViewById(R.id.label_message);
        txt_send_message = findViewById(R.id.txt_send_message);
        btn_disconnect.setEnabled(false);
        btn_send_message.setEnabled(false);
        btn_action1.setEnabled(false);
        btn_action2.setEnabled(false);
        btn_action3.setEnabled(false);
        txt_receive_message.setMovementMethod(ScrollingMovementMethod.getInstance());
        m_handler = new Handler();

        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                m_threadSocket = new Thread(r_buildSocket);
                m_threadSocket.start();
            }
        });
        btn_disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    outputStream.close();
                    m_bufferRead.close();
                    m_socket.close();
                    m_handler.post(new Runnable() {
                        @Override
                        public void run() {
                            btn_connect.setEnabled(true);
                            btn_disconnect.setEnabled(false);
                            btn_send_message.setEnabled(false);
                            btn_action1.setEnabled(false);
                            btn_action2.setEnabled(false);
                            btn_action3.setEnabled(false);
                            txt_status.setTextColor(getResources().getColor(R.color.disconnect_color));
                            txt_status.setText(R.string.status_dis);
                            txt_receive_message.append("Connection has stopped.\n");
                            int offset = txt_receive_message.getLineCount() * txt_receive_message.getLineHeight();
                            if (offset > txt_receive_message.getHeight()) {
                                txt_receive_message.scrollTo(0, offset - txt_receive_message.getHeight());
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        btn_send_message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String message = txt_send_message.getText().toString().trim();
                if (!message.isEmpty()) {
                    new Thread(r_sendMessages).start();
                }
            }
        });

        btn_action1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                message = "1";
                new Thread(r_sendMessages).start();
            }
        });
        btn_action2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                message = "2";
                new Thread(r_sendMessages).start();
            }
        });
        btn_action3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                message = "3";
                new Thread(r_sendMessages).start();
            }
        });
    }
}
