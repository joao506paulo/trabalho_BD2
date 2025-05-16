import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileUploaderGUI extends JFrame implements ActionListener {

    private JButton selectFileButton;
    private JLabel selectedFileLabel;
    private JButton copyFileButton;
    private JTextField destinationFolderField;
    private JLabel destinationFolderLabel;

    private File selectedFile;

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

        copyFileButton = new JButton("Copiar Arquivo");
        copyFileButton.addActionListener(this);
        copyFileButton.setEnabled(false); // Desabilitar até que um arquivo seja selecionado

        add(selectFileButton);
        add(selectedFileLabel);
        add(destinationFolderLabel);
        add(destinationFolderField);
        add(copyFileButton);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == selectFileButton) {
            JFileChooser fileChooser = new JFileChooser();
            // Opcional: definir filtros de tipo de arquivo
            // fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("Arquivos de Texto", "txt"));
            int returnValue = fileChooser.showOpenDialog(this);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                selectedFile = fileChooser.getSelectedFile();
                selectedFileLabel.setText("Arquivo selecionado: " + selectedFile.getAbsolutePath());
                copyFileButton.setEnabled(true);
            }
        } else if (e.getSource() == copyFileButton) {
            if (selectedFile != null) {
                String destinationFolder = destinationFolderField.getText();
                if (destinationFolder == null || destinationFolder.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Por favor, informe a pasta de destino.", "Erro", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    File destinationDir = new File(destinationFolder);
                    if (!destinationDir.exists()) {
                        destinationDir.mkdirs(); // Cria a pasta de destino se não existir
                    }
                    File destinationFile = new File(destinationDir, selectedFile.getName());
                    java.nio.file.Files.copy(selectedFile.toPath(), destinationFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    JOptionPane.showMessageDialog(this, "Arquivo copiado com sucesso para: " + destinationFile.getAbsolutePath(), "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                    selectedFileLabel.setText("Nenhum arquivo selecionado");
                    //selectedFile = null; //preciso verificar por que existe essa linha 
                    copyFileButton.setEnabled(false);

                    // ... dentro do bloco try do actionPerformed do copyFileButton ...

                    String dbUrl = "jdbc:postgresql://localhost:5432/trabalho_bd";
                    String dbUser = "joao";
                    String dbPassword = "1P9a0u1l6o7";

                    try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
     PreparedStatement pstmt = conn.prepareStatement("INSERT INTO arquivos (nome_original, nome_sistema, caminho_copia, data_copia) VALUES (?, ?, ?, ?)")) {

                        String nomeOriginal = selectedFile.getName();
                        String nomeSistema = destinationFile.getName();
                        String caminhoCopia = destinationFile.getAbsolutePath();
                        LocalDateTime now = LocalDateTime.now();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        String dataCopia = now.format(formatter);

                        pstmt.setString(1, nomeOriginal);
                        pstmt.setString(2, nomeSistema);
                        pstmt.setString(3, caminhoCopia);
                        pstmt.setString(4, dataCopia);
                        pstmt.executeUpdate();

                        JOptionPane.showMessageDialog(this, "Arquivo copiado e informações salvas no banco de dados.", "Sucesso", JOptionPane.INFORMATION_MESSAGE);

                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(this, "Erro ao salvar informações no banco de dados: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                        ex.printStackTrace();
                    }


                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Erro ao copiar o arquivo: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Nenhum arquivo selecionado para copiar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FileUploaderGUI());
    }
}