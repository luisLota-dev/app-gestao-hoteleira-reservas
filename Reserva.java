import java.time.LocalDate;

public class Reserva {
    public int id;
    public int idQuarto;
    public int idHospede;
    public int numeroHospedes;
    public LocalDate dataInicio;
    public LocalDate dataFim;
    public boolean ativa;

    public Reserva(int id, int idQuarto, int idHospede, int numeroHospedes, LocalDate dataInicio, LocalDate dataFim,
            boolean ativa) {
        this.id = id;
        this.idQuarto = idQuarto;
        this.idHospede = idHospede;
        this.numeroHospedes = numeroHospedes;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.ativa = ativa;
    }
}
