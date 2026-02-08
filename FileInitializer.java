import java.io.*;

public class FileInitializer {

    public static void ensureSampleFilesExist(
            String fQuartos,
            String fHospedes,
            String fReservas) {

        File fq = new File(fQuartos);
        File fh = new File(fHospedes);
        File fr = new File(fReservas);

        try {
            if (!fq.exists()) {
                try (PrintWriter pw = new PrintWriter(new FileWriter(fq))) {
                    pw.println("id,numero,capacidade,estaOcupado");
                    pw.println("1,101,1,false");
                    pw.println("2,102,1,false");
                    pw.println("3,103,2,false");
                    // resto igual ao teu código
                }
                System.out.println("Criado ficheiro exemplo: " + fQuartos);
            }

            if (!fh.exists()) {
                try (PrintWriter pw = new PrintWriter(new FileWriter(fh))) {
                    pw.println("id,nome,documento");
                    // resto igual
                }
                System.out.println("Criado ficheiro exemplo: " + fHospedes);
            }

            if (!fr.exists()) {
                try (PrintWriter pw = new PrintWriter(new FileWriter(fr))) {
                    pw.println("id,idQuarto,idHospede,numeroHospedes,dataInicio,dataFim,ativa");
                    // resto igual
                }
                System.out.println("Criado ficheiro exemplo: " + fReservas);
            }

        } catch (IOException e) {
            System.out.println("Erro ao criar ficheiros exemplo: " + e.getMessage());
        }
    }
}
