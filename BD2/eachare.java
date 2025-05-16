//João Paulo Santos Torres
//Rodrigo Dorneles

import java.io.*;
import java.net.*;
import java.util.*;
//import java.nio.file.Files;

//Essa é a classe principal do programa
class eachare {

    //Esse método recebe os argumentos e cria as threads do programa
    public static void main (String [] args) throws IOException {
        //recebe os argumentos
        String endereco;
        String vizinhos;
        String diretorio;

        endereco = args[0];
        vizinhos = args[1];
        diretorio = args[2];

        //cria objetos das outras classes que serão usados no programa
        comandos opcoes = new comandos();
        mensagens mensagem = new mensagens();
        relogio r = new relogio();

        //abre um socket na porta especificada no argumento
        String[] endereco_separado = endereco.split(":");
        int porta = Integer.parseInt(endereco_separado[1]);
        ServerSocket serverSocket = new ServerSocket(porta);
        
        //guarda o endereço dos vizinhos presentes do arquivo passado no argumento
        List<Vizinho> lista_de_vizinhos = new ArrayList<Vizinho>();
        //abre o arquivo com a lista de vizinhos
        try (BufferedReader br = new BufferedReader(new FileReader(vizinhos))){
            String linha;
            
            while((linha = br.readLine()) != null){
                lista_de_vizinhos.add(new Vizinho(linha));
                System.out.println("Adicionando novo peer: " + linha + " status OFFILINE");
            }
        } catch (IOException e){
            System.err.println("Erro ao ler o arquivo: " + e.getMessage());
        }
        
        //abre a pasta a ser compartilhada
        File folder = new File(diretorio); 
        File[] arquivos = folder.listFiles();
       
        List<Socket> lista_de_clientes = new ArrayList<Socket>();
       
        //cria a thread responsável pelo menu
        new Thread(() -> menu(lista_de_vizinhos, folder, opcoes, serverSocket, lista_de_clientes, mensagem, endereco, r)).start();

        //recebe conexões e cria threads para lidar com as mensagens recebidas 
        while(true){
            try{
                Socket clientSocket = serverSocket.accept();
                lista_de_clientes.add(clientSocket);
                new Thread(() -> handleConnection(clientSocket, r, lista_de_vizinhos, mensagem, endereco, folder, opcoes)).start();
            } catch (SocketException e){
                System.out.println("Servidor encerrado");
                break;
            }
        }
        
    }

    //Esse método mostra as opções de comandos na tela e chama a classe comandos para executar as ações necessárias
    private static void menu (List<Vizinho> lista_vizinhos, File diretorio, comandos opcoes, ServerSocket serverSocket, List<Socket> lista_clientes, mensagens mensagem, String endereco, relogio r){
        boolean continuar = true; //essa variável é usada para encerrar o programa
        Scanner sc = new Scanner (System.in); //scanner para receber as entradas
        while(continuar){
            
            //exibe na tela as opções
            System.out.println("Escolha um comando:");
            System.out.println("\t [1] Listar peers");
            System.out.println("\t [2] Obter peers");
            System.out.println("\t [3] Listar arquivos locais");
            System.out.println("\t [4] Buscar arquivos");
            System.out.println("\t [5] Exibir estatísticas");
            System.out.println("\t [6] Alterar tamanho de chunk");
            System.out.println("\t [9] Sair");
            System.out.print(">");
            
            //lê a entrada do usuário
            int comando = sc.nextInt();
        
            //chama o método certo presente na classe comandos para atender o comando do usuário
            if(comando == 1){
                opcoes.comando1(lista_vizinhos, mensagem, endereco, r);
            } else if (comando == 2) {
                opcoes.comando2(lista_vizinhos, mensagem, endereco, r);
            } else if (comando == 3) {
                opcoes.comando3(diretorio.listFiles());
            } else if (comando == 4) {
                opcoes.comando4(lista_vizinhos, mensagem, endereco, r);
            } else if (comando == 5) {
                opcoes.comando5();
            } else if (comando == 6) {
                opcoes.comando6();
            } else if (comando == 9) {
                continuar = false;
                try{
                    opcoes.comando9(serverSocket, lista_clientes,lista_vizinhos, mensagem, endereco, r);    
                } catch (IOException e) {
                    System.err.println("Problema ao fechar servidor. " + e.getMessage());
                }
                
            } else {
                System.out.println("Comando inválido");
            }
       }
       //fecha o scanner para não desperdiçar recursos
       sc.close();
       
    }

    //Esse método recebe as mensagens quando uma conexão é feita e executa as ações necessárias para cada mensagem
    private static void handleConnection (Socket clientSocket, relogio r, List<Vizinho> lista, mensagens m, String endereco, File diretorio, comandos opcoes) {
        try(
            BufferedReader in = new BufferedReader (new InputStreamReader(clientSocket.getInputStream()));
        ){
            String inputLine;
            while ((inputLine = in.readLine()) != null){
                System.out.println("Mensagem recebida:" + inputLine);
                String[] partes = inputLine.split(" ");
                int relogio_mensagem = Integer.parseInt(partes[1]);
                r.setRelogio(relogio_mensagem);
                r.incrementaRelogio();
                if(partes[2].equals("HELLO")){
                    System.out.println("Atualizando peer " + partes[0] + " status ONLINE");
                    boolean achou = false;
                    //verifica se quem mandou a mensagem é um vizinho conhecido, se não for o adiciona na lista 
                    for(Vizinho v : lista){
                        if(v.getEndereco().equals(partes[0])){
                            v.setEstado("ONLINE");
                            v.setRelogio(relogio_mensagem);
                            achou = true;
                        }
                    }
                    if(!achou){
                        Vizinho v = new Vizinho(partes[0]);
                        v.setEstado("ONLINE");
                        v.setRelogio(relogio_mensagem);
                        lista.add(v);

                    }
                }
                if(partes[2].equals("BYE")){
                    System.out.println("Atualizando peer " + partes[0] + " status OFFLINE");
                    boolean achou = false;
                    //verifica se quem mandou a mensagem é um vizinho conhecido, se não for o adiciona na lista
                    for(Vizinho v : lista){
                        if(v.getEndereco().equals(partes[0])){
                            v.setEstado("OFFLINE");
                            v.setRelogio(relogio_mensagem);
                            achou = true;
                        }
                    }
                    if(!achou){
                        Vizinho v = new Vizinho(partes[0]);
                        v.setEstado("OFFLINE");
                        v.setRelogio(relogio_mensagem);
                        lista.add(v);

                    }
                }
                if(partes[2].equals("GET_PEERS")){
                    boolean achou = false;
                    for(Vizinho v : lista){
                        if(v.getEndereco().equals(partes[0])){
                            v.setEstado("ONLINE");
                            v.setRelogio(relogio_mensagem);
                            achou = true;
                        }
                    }
                    if(!achou){
                        Vizinho v = new Vizinho(partes[0]);
                        v.setEstado("ONLINE");
                        v.setRelogio(relogio_mensagem);
                        lista.add(v);
                    }
                    //esses 'for' são para não mandar o endereço de quem mandou o GET_PEERS na resposta 
                    for(Vizinho v : lista){
                        if(v.getEndereco().equals(partes[0])){
                            String lista_de_vizinhos = " ";
                            for(Vizinho vi : lista){
                                if(!vi.getEndereco().equals(v.getEndereco())){
                                    lista_de_vizinhos = lista_de_vizinhos + " " +vi.getEndereco()+":"+vi.getEstado()+":"+vi.getRelogio();
                                }
                            }
                            m.mandaMensagem(v, endereco, r, "PEER_LIST " + (lista.size()-1) + lista_de_vizinhos);
                        }
                    }

                }
                if(partes[2].equals("PEER_LIST")){
                    int tamanho_list = Integer.parseInt(partes[3]);
                    int tamanho_vizinhos = lista.size();
                    //antes de adicionar o peer na lista verifica se ele já não era conhecido
                    for(int i = 4; i <= tamanho_list; i++){
                        boolean achou = false;
                        for(int j = 0; j < tamanho_vizinhos; j++){
                            String[] peers = partes[i].split(":");
                            String campo1 = peers[0] + ":" + peers[1];
                            if(campo1.equals(lista.get(j).getEndereco())){
                                String[] estado = partes[i].split(":");
                                int relogio = Integer.parseInt(estado[3]); 
                                lista.get(j).setEstado(estado[2], relogio);
                                lista.get(j).setRelogio(relogio);
                                achou = true;
                                break;
                            }
                        }
                        if(!achou){
                            String[] peers = partes[i].split(":");
                            String campo1 = peers[0] + ":" + peers[1];
                            String estado = peers[2];
                            int relogio = Integer.parseInt(peers[3]);
                            Vizinho peer = new Vizinho(campo1);
                            peer.setEstado(estado, relogio);
                            peer.setRelogio(relogio);
                            lista.add(peer);
                        }
                    }
                }
                if(partes[2].equals("LS")){
                    //verifica se o peer era conhecido e atualiza seu estado
                    boolean achou = false;
                    for(Vizinho v : lista){
                        if(v.getEndereco().equals(partes[0])){
                            v.setEstado("ONLINE");
                            v.setRelogio(relogio_mensagem);
                            achou = true;
                        }
                    }
                    if(!achou){
                        Vizinho v = new Vizinho(partes[0]);
                        v.setEstado("ONLINE");
                        v.setRelogio(relogio_mensagem);
                        lista.add(v);
                    }
                    //executa o comando
                    for(Vizinho v : lista){
                        if(v.getEndereco().equals(partes[0])){
                            String lista_de_arquivos = " ";
                            int n = 0;
                            for (File arquivo : diretorio.listFiles()){
                                lista_de_arquivos = lista_de_arquivos + arquivo.getName() + ":" + arquivo.length() + " ";
                                n = n+1;
                            }
                            m.mandaMensagem(v, endereco, r, "LS_LIST " + n + lista_de_arquivos);
                        }
                    }
                } 
                if(partes[2].equals("LS_LIST")){
                    //verifica se o peer era conhecido e atualiza seu estado
                    boolean achou = false;
                    for(Vizinho v : lista){
                        if(v.getEndereco().equals(partes[0])){
                            v.setEstado("ONLINE");
                            v.setRelogio(relogio_mensagem);
                            achou = true;
                        }
                    }
                    if(!achou){
                        Vizinho v = new Vizinho(partes[0]);
                        v.setEstado("ONLINE");
                        v.setRelogio(relogio_mensagem);
                        lista.add(v);
                    }
                    //executa o comando
                    int tamanho_list = Integer.parseInt(partes[3]);
                    System.out.println(tamanho_list);
                    String[] arquivos = new String[tamanho_list];
                    for(int i = 4; i < partes.length && (i-4) < tamanho_list; i++){
                        arquivos[i-4] = partes[i];
                    }
                    
                    opcoes.executaLS_LIST(partes[0], tamanho_list, arquivos, endereco, r, m, lista); //atualizar os argumentos
                }
                if(partes[2].equals("DL")){
                    //verifica se o peer era conhecido e atualiza seu estado
                    boolean achou = false;
                    for(Vizinho v : lista){
                        if(v.getEndereco().equals(partes[0])){
                            v.setEstado("ONLINE");
                            v.setRelogio(relogio_mensagem);
                            achou = true;
                        }
                    }
                    if(!achou){
                        Vizinho v = new Vizinho(partes[0]);
                        v.setEstado("ONLINE");
                        v.setRelogio(relogio_mensagem);
                        lista.add(v);
                    }
                    //executa o comando
                    for(Vizinho v : lista){
                        if(v.getEndereco().equals(partes[0])){
                            for(File arquivo : diretorio.listFiles()){
                                if(arquivo.getName().equals(partes[3])){
                                    try(FileInputStream fileInputStream = new FileInputStream(arquivo)){
                                        byte[] fileContent = fileInputStream.readAllBytes();
                                        System.out.println(fileContent);
                                        String arquivo_codificado = Base64.getEncoder().encodeToString(fileContent);
                                        m.mandaMensagem(v, endereco, r, "FILE " + arquivo.getName() + " 0 0 " + arquivo_codificado);
                                    } catch(IOException e){
                                        System.out.println("erro ao ler o arquivo");
                                    }
                                }
                            }
                        }
                    }
                }
                if(partes[2].equals("FILE")){
                    //verifica se o peer era conhecido e atualiza seu estado
                    boolean achou = false;
                    for(Vizinho v : lista){
                        if(v.getEndereco().equals(partes[0])){
                            v.setEstado("ONLINE");
                            v.setRelogio(relogio_mensagem);
                            achou = true;
                        }
                    }
                    if(!achou){
                        Vizinho v = new Vizinho(partes[0]);
                        v.setEstado("ONLINE");
                        v.setRelogio(relogio_mensagem);
                        lista.add(v);
                    }
                    //executa o comando
                    String salvar_nome = partes[3];
                    String arquivo_codificado = partes[6];
                    try{
                        byte[] conteudoOriginal = Base64.getDecoder().decode(arquivo_codificado);
                        try(FileOutputStream fos = new FileOutputStream(new File(diretorio, salvar_nome));){
                            fos.write(conteudoOriginal);
                            System.out.println("Download do arquivo " + salvar_nome + " finalizado.");
                        } catch (IOException e){
                            System.err.println("Erro ao escrever arquivo");
                        }
                    }catch (Exception e){
                        System.out.println("Erro ao decodificar o arquivo");
                    }    
                } 
            } 
            
        } catch (IOException e){
            e.printStackTrace();
        } 
    } 
}
