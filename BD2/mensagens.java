import java.io.*;
import java.net.*;
import java.util.*;

//Essa classe é responsável por mandar mensagens de qualquer tipo
class mensagens{
    public void mandaMensagem(Vizinho vizinho, String endereco, relogio r, String tipo){
        try{
            String[] partes = vizinho.getEndereco().split(":");
            String campo1 = partes[0];
            int porta = Integer.parseInt(partes[1]);
            Socket socket = new Socket(campo1, porta);
            System.out.println("conectado a " + vizinho);
            
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            r.incrementaRelogio();
            //monta a mensagem no formato adequado
            String mensagem = endereco + " " + r.getRelogio() + " " + tipo;
            
            //manda a mensagem
            out.println(mensagem);
            
            //mostra para o usuário que a mensagem foi enviada
            System.out.println("Encaminhando mensagem " + mensagem + " para " + vizinho.getEndereco());
            
            //se a mensagem foi recebida, o peer está online
            vizinho.setEstado("ONLINE");

            //fecha os recursos abertos
            out.close();
            socket.close();
        } catch (IOException e){
            //se a mensagem não foi recebida, o peer está offline
            r.incrementaRelogio();
            String mensagem = endereco + " " + r.getRelogio() + " " + tipo;
            System.out.println("Encaminhando mensagem " + mensagem + " para " + vizinho.getEndereco()); //acho que era isso que faltava para a parte 1
            vizinho.setEstado("OFFLINE");
        }
    }
}