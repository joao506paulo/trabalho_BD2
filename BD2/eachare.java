import java.io.*;
import java.net.*;
import java.util.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

//Essa é a classe principal do programa
class eachare {

    // Esse método mostra as opções de comandos na tela e chama a classe comandos
    // para executar as ações necessárias
    private static void menu(List<Vizinho> lista, File folder, comandos opcoes, ServerSocket serverSocket,
            List<Socket> lista_de_clientes, mensagens mensagem, String endereco, relogio r) {
        Scanner sc = new Scanner(System.in);
        int comando;
        boolean sair = false;
        // String meu_endereco = endereco;
        while (!sair) {
            System.out.println("\nMenu de Comandos:");
            System.out.println("[1] Listar vizinhos");
            System.out.println("[2] Procurar vizinhos");
            System.out.println("[3] Listar arquivos no diretório");
            System.out.println("[4] Buscar arquivo");
            System.out.println("[5] Fazer download de arquivo");
            System.out.println("[0] Sair");
            System.out.print("Digite o número do comando desejado: ");
            comando = sc.nextInt();
            sc.nextLine(); // Consumir a quebra de linha após o nextInt()

            switch (comando) {
                case 1:
                    opcoes.comando1(lista, mensagem, endereco, r);
                    break;
                case 2:
                    opcoes.comando2(lista, mensagem, endereco, r);
                    break;
                case 3:
                    opcoes.comando3(folder.listFiles());
                    break;
                case 4:
                    System.out.print("Digite o nome do arquivo a ser buscado: ");
                    String nome_arquivo = sc.nextLine();
                    opcoes.comando4(nome_arquivo, lista, mensagem, endereco, r);
                    break;
                case 5:
                    opcoes.comando5(endereco);
                    break;
                case 0:
                    sair = true;
                    try {
                        serverSocket.close();
                        for (Socket cliente : lista_de_clientes) {
                            cliente.close();
                        }
                    } catch (IOException e) {
                        System.err.println("Erro ao fechar socket: " + e.getMessage());
                    }
                    System.out.println("Encerrando o programa.");
                    break;
                default:
                    System.out.println("Comando inválido. Tente novamente.");
            }
        }
        sc.close();
    }

    // Esse método recebe os argumentos e cria as threads do programa
    public static void main(String[] args) throws IOException {
        // recebe os argumentos
        String endereco;
        String vizinhos;
        String diretorio;

        endereco = args[0];
        vizinhos = args[1];
        diretorio = args[2];

        // cria objetos das outras classes que serão usados no programa
        comandos opcoes = new comandos();
        mensagens mensagem = new mensagens();
        relogio r = new relogio();

        // abre um socket na porta especificada no argumento
        String[] endereco_separado = endereco.split(":");
        int porta = Integer.parseInt(endereco_separado[1]);
        ServerSocket serverSocket = new ServerSocket(porta);

        // guarda o endereço dos vizinhos presentes do arquivo passado no argumento
        List<Vizinho> lista_de_vizinhos = new ArrayList<Vizinho>();
        // abre o arquivo com a lista de vizinhos
        try (BufferedReader br = new BufferedReader(new FileReader(vizinhos))) {
            String linha;

            while ((linha = br.readLine()) != null) {
                lista_de_vizinhos.add(new Vizinho(linha));
                System.out.println("Adicionando novo peer: " + linha + " status OFFILINE");
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo: " + e.getMessage());
        }

        // abre a pasta a ser compartilhada
        File folder = new File(diretorio);
        File[] arquivos = folder.listFiles();

        List<Socket> lista_de_clientes = new ArrayList<Socket>();

        // cria a thread responsável pelo menu
        new Thread(() -> menu(lista_de_vizinhos, folder, opcoes, serverSocket, lista_de_clientes, mensagem, endereco, r))
                .start();

        // recebe conexões e cria threads para lidar com as mensagens recebidas
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                lista_de_clientes.add(clientSocket);
                new Thread(() -> handleConnection(clientSocket, r, lista_de_vizinhos, mensagem, endereco, folder, opcoes))
                        .start();
            } catch (SocketException e) {
                System.out.println("Servidor encerrado");
                break;
            }
        }
    }

    // Esse método mostra as opções de comandos na tela e chama a classe comandos
    // para executar as ações necessárias
    private static void handleConnection(Socket clientSocket, relogio r, List<Vizinho> lista_de_vizinhos, mensagens mensagem,
            String enderecoServidor, File diretorio, comandos opcoes) {
        try {
            // Configurar streams de entrada e saída
            InputStream in = clientSocket.getInputStream();
            DataInputStream dis = new DataInputStream(in);
            OutputStream out = clientSocket.getOutputStream();
            DataOutputStream dos = new DataOutputStream(out);
            // Ler o comando do cliente
            String comando = dis.readUTF();

            System.out.println("Comando recebido: " + comando);

            if (comando.equals("UPLOAD")) {
                // Receber informações do arquivo
                String nomeArquivo = dis.readUTF();
                long tamanhoArquivo = dis.readLong();
                String pastaDestino = dis.readUTF();

                // Criar diretório de destino se não existir
                File destino = new File(pastaDestino);
                if (!destino.exists()) {
                    destino.mkdirs(); // Cria o diretório e seus pais, se necessário
                }

                // Receber o conteúdo do arquivo
                byte[] buffer = new byte[(int) tamanhoArquivo];
                dis.readFully(buffer); // Lê exatamente o número de bytes do arquivo

                // Salvar o arquivo no sistema de arquivos
                File arquivoSalvo = new File(destino, nomeArquivo);
                try (FileOutputStream fos = new FileOutputStream(arquivoSalvo)) {
                    fos.write(buffer);
                    System.out.println("Arquivo " + nomeArquivo + " salvo em " + arquivoSalvo.getAbsolutePath());
                } catch (IOException e) {
                    System.err.println("Erro ao salvar o arquivo: " + e.getMessage());
                    dos.writeUTF("UPLOAD_ERROR: Falha ao salvar o arquivo no servidor.");
                    dos.flush();
                    clientSocket.close();
                    return; // Importante: retornar para evitar o restante do processamento
                }

                // Salvar informações no banco de dados
                String url = "jdbc:postgresql://localhost:5432/trabalho_bd"; // Alterar
                String usuario = "joao"; // Alterar
                String senha = "1P9a0u1l6o7"; // Alterar

                try (Connection conn = DriverManager.getConnection(url, usuario, senha)) { 
                    //vou ter que modificar esse trecho manualmente e colocar isso na apresentação
                    String sql = "INSERT INTO arquivos (nome_original, nome_sistema, caminho_completo, data_upload) VALUES (?, ?, ?, ?)"; //alterar aqui
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, nomeArquivo);
                    pstmt.setString(2, arquivoSalvo.getName());
                    pstmt.setString(3, arquivoSalvo.getAbsolutePath());
                    LocalDateTime now = LocalDateTime.now();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    String dataUpload = now.format(formatter);
                    pstmt.setString(4, dataUpload);
                    pstmt.executeUpdate();

                    System.out.println("Informações do arquivo salvas no banco de dados.");
                    dos.writeUTF("UPLOAD_OK"); // Envia confirmação de sucesso
                    dos.flush();

                } catch (SQLException e) {
                    System.err.println("Erro ao salvar informações no banco de dados: " + e.getMessage());
                    e.printStackTrace(); // Para diagnóstico completo
                    dos.writeUTF("UPLOAD_ERROR: Erro ao salvar no banco de dados: " + e.getMessage());
                    dos.flush();
                    clientSocket.close();
                    return;
                }
            } else {
                // Lógica existente para outros comandos
                String linha = comando;
                String[] partes = linha.split(" ");
                String tipo_mensagem = partes[2];
                int relogio_mensagem = Integer.parseInt(partes[1]);
                r.setRelogio(relogio_mensagem);

                if (tipo_mensagem.equals("HELLO")) {
                    boolean achou = false;
                    for (Vizinho v : lista_de_vizinhos) {
                        if (v.getEndereco().equals(partes[0])) {
                            v.setEstado("ONLINE");
                            v.setRelogio(relogio_mensagem);
                            achou = true;
                        }
                    }
                    if (!achou) {
                        Vizinho v = new Vizinho(partes[0]);
                        v.setEstado("ONLINE");
                        v.setRelogio(relogio_mensagem);
                        lista_de_vizinhos.add(v);
                    }
                } else if (tipo_mensagem.equals("GET_PEERS")) {
                    String lista_peers = "";
                    for (Vizinho v : lista_de_vizinhos) {
                        if (v.getEstado().equals("ONLINE") && !v.getEndereco().equals(partes[0])) {
                            lista_peers = lista_peers + v.getEndereco() + ",";
                        }
                    }
                    mensagem.mandaMensagem(new Vizinho(partes[0]), enderecoServidor, r, "PEERS " + lista_peers);
                } else if (tipo_mensagem.equals("PEERS")) {
                    if (partes.length > 3) {
                        String[] peers = partes[3].split(",");
                        for (String peer : peers) {
                            if (!peer.isEmpty()) {
                                boolean achou = false;
                                for (Vizinho v : lista_de_vizinhos) {
                                    if (v.getEndereco().equals(peer)) {
                                        achou = true;
                                        break;
                                    }
                                }
                                if (!achou) {
                                    lista_de_vizinhos.add(new Vizinho(peer));
                                    System.out.println("Novo vizinho encontrado: " + peer + " status OFFLINE");
                                }
                            }
                        }
                    }
                } else if (tipo_mensagem.equals("LS")) {
                    String lista_arquivos = "";
                    for (File arquivo : diretorio.listFiles()) {
                        String nome_arquivo = arquivo.getName();
                        long tamanho_arquivo = arquivo.length();
                        lista_arquivos = lista_arquivos + nome_arquivo + ":" + tamanho_arquivo + ",";
                    }
                    mensagem.mandaMensagem(new Vizinho(partes[0]), enderecoServidor, r, "FILES " + lista_arquivos);
                } else if (tipo_mensagem.equals("FILES")) {
                    if (partes.length > 3) {
                        opcoes.setLs(opcoes.getLs() - 1);
                        String endereco_vizinho = partes[0];
                        String[] arquivos = partes[3].split(",");
                        for (String arquivo : arquivos) {
                            // adiciona as informações em uma estrutura
                            if (!arquivo.isEmpty())
                                opcoes.getLista_de_arquivos().add(new arquivos_vizinhos(arquivo, endereco_vizinho));
                        }
                        if (opcoes.getLs() == 0) {
                            // mostra na tela
                            System.out.println("Arquivos encontrado na rede:");
                            System.out.println("Nome | Tamanho | Peer");
                            System.out.println("[ 0] <Cancelar> | |");
                            int i = 1;
                            for (arquivos_vizinhos arquivo : opcoes.getLista_de_arquivos()) {
                                System.out.println("[ " + i + "] " + arquivo.getNome() + " | " + arquivo.getTamanho() + " | "
                                        + arquivo.getNomeVizinho());
                                i += 1;
                            }
                            System.out.println("Digite o numero do arquivo para fazer o download:");
                            System.out.print(">");
                            Scanner sc = new Scanner(System.in);
                            int comando_download = sc.nextInt();
                            int j;
                            for (j = 0; j < i; j++) {
                                if (comando_download == 0) {
                                    break;
                                }
                                if (comando_download == j) {
                                    for (Vizinho v : lista_de_vizinhos) {
                                        if (v.getEndereco().equals(opcoes.getLista_de_arquivos().get(j - 1).getNomeVizinho())) {
                                            mensagem.mandaMensagem(v, enderecoServidor, r, "DL "
                                                    + opcoes.getLista_de_arquivos().get(j - 1).getNome() + " 0 0"); // atualizar
                                        }
                                    }
                                }
                            }
                            sc.close();
                        }
                    }
                } else if (tipo_mensagem.equals("DL")) {
                    String nome_arquivo_download = partes[3];
                    String salvar_nome = nome_arquivo_download;
                    for (File arquivo : diretorio.listFiles()) {
                        if (arquivo.getName().equals(nome_arquivo_download)) {
                            // Enviar arquivo solicitado
                            long tamanho_arquivo = arquivo.length();
                            String endereco_cliente = partes[0];
                            String[] ip_porta = endereco_cliente.split(":");
                            String ip_cliente = ip_porta[0];
                            int porta_cliente = Integer.parseInt(ip_porta[1]);
                            try (Socket socket_envio = new Socket(ip_cliente, porta_cliente)) {
                                OutputStream output = socket_envio.getOutputStream();
                                DataOutputStream dos_envio = new DataOutputStream(output);
                                dos_envio.writeUTF(enderecoServidor + " " + r.getRelogio() + " " + "ARQUIVO");
                                dos_envio.writeUTF(nome_arquivo_download);
                                dos_envio.writeLong(tamanho_arquivo);
                                dos_envio.writeUTF(salvar_nome);

                                FileInputStream fis = new FileInputStream(arquivo);
                                byte[] buffer = new byte[(int) tamanho_arquivo];
                                fis.read(buffer);
                                String arquivo_codificado = Base64.getEncoder().encodeToString(buffer);
                                dos_envio.writeUTF(arquivo_codificado);

                                fis.close();
                                dos_envio.close();
                                socket_envio.close();
                            } catch (Exception e) {
                                System.err.println("Erro ao enviar arquivo para download: " + e.getMessage());
                            }
                        }
                    }
                } else if (tipo_mensagem.equals("ARQUIVO")) {

                    String salvar_nome = partes[3];
                    String arquivo_codificado = partes[6];
                    try {
                        byte[] conteudoOriginal = Base64.getDecoder().decode(arquivo_codificado);
                        try (FileOutputStream fos = new FileOutputStream(new File(diretorio, salvar_nome));) {
                            fos.write(conteudoOriginal);
                            System.out.println("Download do arquivo " + salvar_nome + " finalizado.");
                        } catch (IOException e) {
                            System.err.println("Erro ao escrever arquivo");
                        }
                    } catch (Exception e) {
                        System.out.println("Erro ao decodificar o arquivo");
                    }
                }
            }
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
