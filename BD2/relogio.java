import java.io.*;
import java.net.*;
import java.util.*;

//Essa classe é responsável por controlar o funcionamento do relógio local
class relogio {
    private int relogio = 0;
    public int getRelogio(){
        return this.relogio;
    }
    public void incrementaRelogio(){
        this.relogio = this.relogio+1;
        System.out.println("=> Atualizando relogio para " + this.relogio);
    }
    public void setRelogio(int mensagem){
        this.relogio = (this.relogio < mensagem)? mensagem : this.relogio;
    }
}