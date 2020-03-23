package top.ketra;

import java.awt.AWTEvent;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.io.*;
import javax.swing.JOptionPane;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFrame;
import org.java_websocket.client.*;
import org.java_websocket.handshake.ServerHandshake;

import top.ketra.Draws.Draw;

public class Board extends JFrame implements MouseMotionListener, MouseListener {

    private static final long serialVersionUID = -2194947707840582384L;

    WebSocketClient webSocketClient;
    Pattern pattern = Pattern.compile(
            "([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]{1,2}|2[0-4][0-9]|25[0-5])");

    private Map<String, Draw> draws = new ConcurrentHashMap<String, Draw>();

    public Board() {
        setBounds(400, 400, 500, 350);
        setTitle("画板主界面");
        setVisible(true);
        initConnect();
        enableEvents(AWTEvent.MOUSE_EVENT_MASK);
        addMouseMotionListener(this);
        addMouseListener(this);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    private void requireAddr() {
        while (true) {
            Global.address = JOptionPane.showInputDialog(this, "请输入服务器 IP 地址", Global.address);
            Matcher matcher = pattern.matcher(Global.address);
            if (matcher.matches()) {
                break;
            } else {
                JOptionPane.showMessageDialog(this, "格式错误", "提示", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void initConnect() {
        Board that = this;
        while (true) {
            requireAddr();
            try {
                webSocketClient = new WebSocketClient(new URI("ws://" + Global.address + Global.port)) {
                    @Override
                    public void onOpen(ServerHandshake handshakedata) {
                    }

                    @Override
                    public void onMessage(String message) {
                    }

                    @Override
                    public void onMessage(ByteBuffer bytes) {
                        that.processMessage(bytes);
                        super.onMessage(bytes);
                    }

                    @Override
                    public void onClose(int code, String reason, boolean remote) {
                        JOptionPane.showMessageDialog(null, "已与服务器断开连接，请退出后重新连接", "提示", JOptionPane.ERROR_MESSAGE);
                    }

                    @Override
                    public void onError(Exception ex) {

                    }
                };
                webSocketClient.connectBlocking();
                break;
            } catch (URISyntaxException | InterruptedException e) {
                JOptionPane.showMessageDialog(this, "连接失败，点击确定重新连接", "提示", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    void processMessage(ByteBuffer bytes) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes.array());
        ObjectInputStream objectInputStream = null;
        try {
            objectInputStream = new ObjectInputStream(byteArrayInputStream);
            Object object = objectInputStream.readObject();
            if (object instanceof Draw) {
                Draw draw = (Draw) object;
                draws.put(draw.getUuid(), draw);
                repaint();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (byteArrayInputStream != null) {
                    byteArrayInputStream.close();
                }
                if (objectInputStream != null) {
                    objectInputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        draws.forEach((k, v) -> v.paint(g));
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (Global.currentDraw != null && webSocketClient.isOpen()) {
            Global.currentDraw.onMouseMove(e.getPoint());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = null;
            try {
                objectOutputStream = new ObjectOutputStream(outputStream);
                objectOutputStream.writeObject(Global.currentDraw);
                webSocketClient.send(outputStream.toByteArray());
            } catch (IOException e1) {
                e1.printStackTrace();
            } finally {
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                    if (objectOutputStream != null) {
                        objectOutputStream.close();
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Global.createDraw(e.getPoint());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        mouseDragged(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {
    }

    @Override
    public void mouseExited(MouseEvent arg0) {
    }
}