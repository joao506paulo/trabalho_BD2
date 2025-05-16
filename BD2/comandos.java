import java.io.*;
import java.net.*;
import java.util.*;

//Essa classe possui métodos para atender a cada um dos comandos disponíveis para o usuário
class comandos {
    private int ls;
    private List<arquivos_vizinhos> lista_de_arquivos = new LinkedList<>();
    private boolean bloqueia = true;

    public boolean getBloqueia() {
        return this.bloqueia;
    }

    public void comando1(List<Vizinho> lista, mensagens m, String endereco, relogio r) {
        int i = 1;
        Scanner sc = new Scanner(System.in);
        System.out.println("[0] voltar para o menu anterior");
        for (Vizinho vizinho : lista) {
            System.out.print("[" + i + "]");
            vizinho.imprime();
            System.out.print("\n");
            i++;
        }
        System.out.print(">");
        int comando = sc.nextInt();
        int j;
        for (j = 0; j <= i; j++) {
            if (comando == 0) {
                break;
            }
            if (comando == j) {
                m.mandaMensagem(lista.get(j - 1), endereco, r, "HELLO");
            }
        }
    }

    public void comando2(List<Vizinho> lista, mensagens m, String endereco, relogio r) {
        for (Vizinho v : lista) {
            m.mandaMensagem(v, endereco, r, "GET_PEERS");
        }
    }

    public void comando3(File[] arquivos) {
        if (arquivos != null) {
            System.out.println("Arquivos no diretório:");
            for (File arquivo : arquivos) {
                System.out.println(arquivo.getName());
            }
        } else {
            System.out.println("Diretório vazio ou inválido.");
        }
    }

    public void comando4(String nome_arquivo, List<Vizinho> lista, mensagens mensagem, String endereco, relogio r) {
        for (Vizinho v : lista) {
            mensagem.mandaMensagem(v, endereco, r, "FIND " + nome_arquivo);
        }
    }

    public void comando5(String endereco) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Digite o endereço do arquivo a ser baixado (ip:porta/nome_do_arquivo): ");
        String arquivoParaBaixar = sc.nextLine();
        String[] partes = arquivoParaBaixar.split("/");
        if (partes.length == 2) {
            String enderecoPeer = partes[0];
            String nomeArquivo = partes[1];
            // Aqui você chamaria o método para iniciar o download do arquivo
            System.out.println("Iniciando download do arquivo " + nomeArquivo + " do peer " + enderecoPeer);
        } else {
            System.out.println("Formato inválido. Use ip:porta/nome_do_arquivo.");
        }
        sc.close();
    }

    public int getLs() {
        return this.ls;
    }

    public void setLs(int ls) {
        this.ls = ls;
    }

    public List<arquivos_vizinhos> getLista_de_arquivos() {
        return this.lista_de_arquivos;
    }
}
