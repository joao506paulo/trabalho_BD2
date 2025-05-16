import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class FileUploaderGUI extends JFrame implements ActionListener {

    private JButton selectFileButton;
    private JLabel selectedFileLabel;
    private JButton sendFileButton; // Renamed from copyFileButton
    private JTextField destinationFolderField;
    private JLabel destinationFolderLabel;

    private File selectedFile;
    private String serverAddress = "localhost:8080"; // Endereço e porta do servidor eachare

    public FileUploaderGUI() {
        setTitle("File Uploader");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout());

        selectFileButton = new JButton("Selecionar Arquivo");
        selectFileButton.addActionListener(this);

        selectedFileLabel = new JLabel("Nenhum arquivo selecionado");

        destinationFolderLabel = new JLabel("Pasta de Destino:");
        destinationFolderField = new JTextField(20);
        destinationFolderField.setText("./user"); // Defina um caminho padrão

        sendFileButton = new JButton("Enviar Arquivo"); // Renamed button
        sendFileButton.addActionListener(this);
        sendFileButton.setEnabled(false); // Desabilitar até que um arquivo seja selecionado

        add(selectFileButton);
        add(selectedFileLabel);
        add(destinationFolderLabel);
        add(destinationFolderField);
        add(sendFileButton); // Added sendFileButton
        //pack(); // Não use pack() para ter mais controle sobre o tamanho
        setSize(400, 200); // Defina um tamanho adequado para a janela
        setLocationRelativeTo(null); // Centraliza a janela
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == selectFileButton) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("Todos os Arquivos", "*.*"));
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                selectedFileLabel.setText("Arquivo selecionado: " + selectedFile.getName());
                sendFileButton.setEnabled(true); // Habilitar o botão de enviar
            }
        } else if (e.getSource() == sendFileButton) { // Changed to sendFileButton
            if (selectedFile != null) {
                try {
                    // Conectar ao servidor (eachare)
                    String[] addressParts = serverAddress.split(":");
                    String host = addressParts[0];
                    int port = Integer.parseInt(addressParts[1]);
                    Socket socket = new Socket(host, port);
                    System.out.println("Conectado ao servidor " + serverAddress);

                    // Obter fluxo de saída para enviar dados para o servidor
                    OutputStream out = socket.getOutputStream();
                    DataOutputStream dos = new DataOutputStream(out);

                    // Ler o arquivo e enviar para o servidor
                    FileInputStream fis = new FileInputStream(selectedFile);
                    byte[] buffer = new byte[(int) selectedFile.length()]; // Lê todo o arquivo de uma vez
                    fis.read(buffer);

                    // Enviar metadados do arquivo primeiro
                    dos.writeUTF("UPLOAD"); // Comando para o servidor
                    dos.writeUTF(selectedFile.getName());
                    dos.writeLong(selectedFile.length());
                    dos.writeUTF(destinationFolderField.getText()); // Envia o destino

                    // Enviar o conteúdo do arquivo
                    dos.write(buffer);
                    dos.flush();

                    // Receber resposta do servidor
                    InputStream in = socket.getInputStream();
                    DataInputStream dis = new DataInputStream(in);
                    String response = dis.readUTF();
                    System.out.println("Resposta do servidor: " + response);

                    if (response.equals("UPLOAD_OK")) {
                        JOptionPane.showMessageDialog(this, "Arquivo enviado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(this, "Falha ao enviar arquivo: " + response, "Erro", JOptionPane.ERROR_MESSAGE);
                    }

                    // Fechar recursos
                    fis.close();
                    dos.close();
                    socket.close();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Erro de E/S: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Nenhum arquivo selecionado para enviar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FileUploaderGUI());
    }
}

