import java.io.*;
import java.net.*;
import java.util.*;

//Essa classe possui métodos para atender a cada um dos comandos disponíveis para o usuário
class comandos {
    private int ls;
    private List<arquivos_vizinhos> lista_de_arquivos = new LinkedList<>();
    private boolean bloqueia = true;

    public boolean getBloqueia(){
        return this.bloqueia;
    }

    public void comando1 (List<Vizinho> lista, mensagens m, String endereco, relogio r){
        int i = 1;
        Scanner sc = new Scanner(System.in);
        System.out.println("[0] voltar para o menu anterior");
        for(Vizinho vizinho : lista){
            System.out.print("[" + i +"]");
            vizinho.imprime();
            System.out.print("\n");
            i++;
        }
        System.out.print(">");
        int comando = sc.nextInt();
        int j;
        for(j=0; j <= i; j++){
            if(comando == 0){
                break;
            }
            if(comando == j){
                m.mandaMensagem(lista.get(j-1), endereco, r, "HELLO"); 
            }
        }
    }
    public void comando2 (List<Vizinho> lista, mensagens m, String endereco, relogio r){
        for(Vizinho v : lista){
            m.mandaMensagem(v, endereco, r, "GET_PEERS");
        }  
    }
    public void comando3 (File[] arquivos){
        if (arquivos != null){
            for(File arquivo : arquivos){
                if(arquivo.isFile()){
                    System.out.println("   " + arquivo.getName());
                }
            }
        }

    }
    public void comando4 (List<Vizinho> lista, mensagens m, String endereco, relogio r){
        int mensagens_enviadas = 0;
        lista_de_arquivos.clear();
        for(Vizinho v : lista){
            if(v.getEstado().equals("ONLINE")){
                m.mandaMensagem(v, endereco, r, "LS");
                mensagens_enviadas = mensagens_enviadas +1;
            }
        }  
        this.ls = mensagens_enviadas;
        while(bloqueia){
            try{
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        //System.out.println("voltou a bloquear");
        this.bloqueia = true;
    }  
    public void comando5 (){
        //será implemantado na outra parte do ep
        System.out.println("Comando ainda não implementado.");   
    }    
    public void comando6 (){
        //será implementado na outra parte do ep
        System.out.println("Comando ainda não implementado.");   
    }    
    public void comando9 (ServerSocket serverSocket, List<Socket> clientes, List<Vizinho> lista, mensagens m, String endereco, relogio r) throws IOException{
        //manda a mensagem tipo bye
        for(Vizinho v: lista){
            if(v.getEstado().equals("ONLINE")){
                m.mandaMensagem(v, endereco, r, "BYE");
            }
        }
        //fecha os recursos abertos
        for(Socket cliente : clientes){           
            cliente.close();
        }
        serverSocket.close();
    }
    public void executaLS_LIST(String endereco_vizinho,int tamanho_list, String[] arquivos, String meu_endereco, relogio r, mensagens m, List<Vizinho> lista){
        this.ls = this.ls-1;
        for(String arquivo :  arquivos){
            //adiciona as informações em uma estrutura
            this.lista_de_arquivos.add(new arquivos_vizinhos(arquivo, endereco_vizinho));
        }
        if(this.ls == 0){
            //mostra na tela
            System.out.println("Arquivos encontrado na rede:");
            System.out.println("Nome | Tamanho | Peer");
            System.out.println("[ 0] <Cancelar> | |");
            int i = 1;
            for(arquivos_vizinhos arquivo : this.lista_de_arquivos){
                System.out.println("[ " + i + "] " + arquivo.getNome() + " | " + arquivo.getTamanho() + " | " + arquivo.getNomeVizinho() );
                i += 1;
            }
            System.out.println("Digite o numero do arquivo para fazer o download:");
            System.out.print(">");
            Scanner sc = new Scanner(System.in);
            int comando = sc.nextInt();
            int j;
            for(j=0; j < i; j++){
                if(comando == 0){
                    break;
                }
                if(comando == j){
                    for(Vizinho v : lista){
                        if(v.getEndereco().equals(lista_de_arquivos.get(j-1).getNomeVizinho())){
                            m.mandaMensagem(v, meu_endereco, r, "DL " + lista_de_arquivos.get(j-1).getNome() + " 0 0"); //atualizar
                        }
                    }
                }
            }
            this.bloqueia = false;
        }
    }
}