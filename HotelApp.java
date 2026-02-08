import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class HotelApp {
    private static final int MAX_QUARTOS = 200;
    private static final int MAX_HOSPEDES = 1000;
    private static final int MAX_RESERVAS = 1000;

    private static Quarto[] quartos = new Quarto[MAX_QUARTOS];
    private static Hospede[] hospedes = new Hospede[MAX_HOSPEDES];
    private static Reserva[] reservas = new Reserva[MAX_RESERVAS];

    private static int nQuartos = 0;
    private static int nHospedes = 0;
    private static int nReservas = 0;

    private static int nextHospedeId = 1;
    private static int nextReservaId = 1;

    private static final String F_QUARTOS = "quartos.csv";
    private static final String F_HOSPEDES = "hospedes.csv";
    private static final String F_RESERVAS = "reservas.csv";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // CREATE NEW FILE IF NOT EXISTS
        FileInitializer.ensureSampleFilesExist(
                F_QUARTOS,
                F_HOSPEDES,
                F_RESERVAS);

        // LOAD DATA
        loadQuartos(F_QUARTOS);
        loadHospedes(F_HOSPEDES);
        loadReservas(F_RESERVAS);

        // UPDATE ROOM OCCUPANCY
        atualizarOcupacaoComBaseNasReservas();

        // MENU LOOP
        boolean menuActive = true;
        while (menuActive) {
            System.out.println("\n=== GESTÃO HOTEL (Consola) ===");
            System.out.println("1) Quartos");
            System.out.println("2) Hóspedes");
            System.out.println("3) Reservas");
            System.out.println("0) Sair (guardar)");
            System.out.print("Opção: ");

            int op = readInt(sc);
            switch (op) {
                case 1 -> menuQuartos(sc);
                case 2 -> menuHospedes(sc);
                case 3 -> menuReservas(sc);
                case 0 -> {
                    menuActive = false;

                    // SAVE DATA
                    saveHospedes(F_HOSPEDES);
                    saveReservas(F_RESERVAS);
                    System.out.println("Dados guardados. A terminar...");
                }
                default -> System.out.println("Opção inválida.");
            }
        }

        sc.close();
    }

    // ===================== MENUS =====================

    private static void menuQuartos(Scanner sc) {
        boolean back = false;
        while (!back) {
            atualizarOcupacaoComBaseNasReservas();

            System.out.println("\n--- MENU QUARTOS ---");
            System.out.println("1) Listar todos os quartos");
            System.out.println("2) Listar quartos livres");
            System.out.println("3) Listar quartos ocupados (com ocupação)");
            System.out.println("4) Ver quarto específico (reservas do quarto)");
            System.out.println("0) Voltar");
            System.out.print("Opção: ");

            int op = readInt(sc);
            switch (op) {
                case 1 -> listarTodosQuartos();
                case 2 -> listarQuartosLivres();
                case 3 -> listarQuartosOcupadosComDetalhe();
                case 4 -> listarQuartoEspecifico(sc);
                case 0 -> back = true;
                default -> System.out.println("Opção inválida.");
            }
        }
    }

    private static void menuHospedes(Scanner sc) {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- MENU HÓSPEDES ---");
            System.out.println("1) Listar hóspedes");
            System.out.println("2) Procurar hóspede por documento");
            System.out.println("3) Editar hóspede (nome/documento)");
            System.out.println("0) Voltar");
            System.out.print("Opção: ");

            int op = readInt(sc);
            switch (op) {
                case 1 -> listarHospedes();
                case 2 -> procurarHospedePorDocumento(sc);
                case 3 -> editarHospede(sc);
                case 0 -> back = true;
                default -> System.out.println("Opção inválida.");
            }
        }
    }

    private static void menuReservas(Scanner sc) {
        boolean back = false;
        while (!back) {
            atualizarOcupacaoComBaseNasReservas();

            System.out.println("\n--- MENU RESERVAS ---");
            System.out.println("1) Criar reserva (encontrar quarto adequado)");
            System.out.println("2) Listar todas as reservas");
            System.out.println("3) Listar reservas por quarto (presentes ou futuras)");
            System.out.println("4) Listar reservas por hóspede (presentes ou futuras)");
            System.out.println("5) Editar reserva");
            System.out.println("6) Cancelar reserva");
            System.out.println("0) Voltar");
            System.out.print("Opção: ");

            int op = readInt(sc);
            switch (op) {
                case 1 -> criarReserva(sc);
                case 2 -> listarTodasReservas();
                case 3 -> listarReservasPorQuarto(sc);
                case 4 -> listarReservasPorHospede(sc);
                case 5 -> editarReserva(sc);
                case 6 -> cancelarReserva(sc);
                case 0 -> back = true;
                default -> System.out.println("Opção inválida.");
            }
        }
    }

    // ===================== QUARTOS =====================

    private static void listarTodosQuartos() {
        if (nQuartos == 0) {
            System.out.println("Sem quartos carregados.");
            return;
        }
        System.out.println("\nID | Nº | Capacidade | Ocupado?");
        for (int i = 0; i < nQuartos; i++) {
            Quarto q = quartos[i];
            System.out.printf("%d | %d | %d | %s%n",
                    q.id, q.numero, q.capacidade, q.estaOcupado ? "SIM" : "NÃO");
        }
    }

    private static void listarQuartosLivres() {
        boolean encontrou = false;
        System.out.println("\nQuartos livres:");
        for (int i = 0; i < nQuartos; i++) {
            if (!quartos[i].estaOcupado) {
                Quarto q = quartos[i];
                System.out.printf("ID %d | Nº %d | Cap %d%n", q.id, q.numero, q.capacidade);
                encontrou = true;
            }
        }
        if (!encontrou)
            System.out.println("(nenhum)");
    }

    private static void listarQuartosOcupadosComDetalhe() {
        LocalDate hoje = LocalDate.now();
        boolean encontrou = false;

        System.out.println("\nQuartos ocupados (hoje = " + hoje + "):");
        for (int i = 0; i < nQuartos; i++) {
            Quarto q = quartos[i];
            if (q.estaOcupado) {
                Reserva r = reservaAtivaPresenteDoQuarto(q.id, hoje);
                if (r != null) {
                    Hospede h = findHospedeById(r.idHospede);
                    System.out.printf("Quarto Nº %d (ID %d) | Cap %d | Hóspede: %s | %s a %s%n",
                            q.numero, q.id, q.capacidade,
                            (h != null ? h.nome + " [" + h.documento + "]" : "desconhecido"),
                            r.dataInicio, r.dataFim);
                } else {
                    System.out.printf("Quarto Nº %d (ID %d) | Cap %d | Ocupado (reserva não encontrada)%n",
                            q.numero, q.id, q.capacidade);
                }
                encontrou = true;
            }
        }
        if (!encontrou)
            System.out.println("(nenhum)");
    }

    private static void listarQuartoEspecifico(Scanner sc) {
        System.out.print("Digite o ID do quarto: ");
        int id = readInt(sc);
        Quarto q = findQuartoById(id);
        if (q == null) {
            System.out.println("Quarto não encontrado.");
            return;
        }
        System.out.printf("Quarto ID %d | Nº %d | Cap %d | Ocupado? %s%n",
                q.id, q.numero, q.capacidade, q.estaOcupado ? "SIM" : "NÃO");

        System.out.println("Reservas desse quarto (ativas):");
        boolean encontrou = false;
        for (int i = 0; i < nReservas; i++) {
            Reserva r = reservas[i];
            if (r.ativa && r.idQuarto == q.id) {
                Hospede h = findHospedeById(r.idHospede);
                System.out.printf("Reserva %d | Hóspede: %s | %d hóspedes | %s a %s%n",
                        r.id,
                        (h != null ? h.nome + " [" + h.documento + "]" : "desconhecido"),
                        r.numeroHospedes,
                        r.dataInicio, r.dataFim);
                encontrou = true;
            }
        }
        if (!encontrou)
            System.out.println("(nenhuma)");
    }

    // ===================== HÓSPEDES =====================

    private static void listarHospedes() {
        if (nHospedes == 0) {
            System.out.println("Sem hóspedes.");
            return;
        }
        System.out.println("\nID | Nome | Documento");
        for (int i = 0; i < nHospedes; i++) {
            Hospede h = hospedes[i];
            System.out.printf("%d | %s | %s%n", h.id, h.nome, h.documento);
        }
    }

    private static void procurarHospedePorDocumento(Scanner sc) {
        System.out.print("Documento: ");
        String doc = readNonEmptyLine(sc);
        Hospede h = findHospedeByDocumento(doc);
        if (h == null) {
            System.out.println("Hóspede não encontrado.");
        } else {
            System.out.printf("Encontrado: ID %d | %s | %s%n", h.id, h.nome, h.documento);
        }
    }

    private static void editarHospede(Scanner sc) {
        System.out.print("Documento do hóspede a editar: ");
        String doc = readNonEmptyLine(sc);
        Hospede h = findHospedeByDocumento(doc);
        if (h == null) {
            System.out.println("Hóspede não encontrado.");
            return;
        }

        System.out.println("Hóspede atual: " + h.nome + " | " + h.documento);
        System.out.print("Novo nome (ENTER para manter): ");
        String novoNome = sc.nextLine().trim();

        System.out.print("Novo documento (ENTER para manter): ");
        String novoDoc = sc.nextLine().trim();

        if (!novoNome.isEmpty())
            h.nome = novoNome;

        if (!novoDoc.isEmpty()) {
            if (findHospedeByDocumento(novoDoc) != null && !novoDoc.equalsIgnoreCase(h.documento)) {
                System.out.println("Falha: já existe um hóspede com esse documento.");
                return;
            }
            h.documento = novoDoc;
        }

        System.out.println("Hóspede atualizado com sucesso.");
    }

    // ===================== RESERVAS =====================

    private static void criarReserva(Scanner sc) {
        if (nReservas >= MAX_RESERVAS) {
            System.out.println("Falha: limite de reservas atingido.");
            return;
        }

        System.out.print("Documento do hóspede: ");
        String doc = readNonEmptyLine(sc);

        Hospede h = findHospedeByDocumento(doc);
        if (h == null) {
            System.out.println("Hóspede não existe. Vamos criar.");
            if (nHospedes >= MAX_HOSPEDES) {
                System.out.println("Falha: limite de hóspedes atingido.");
                return;
            }
            System.out.print("Nome do hóspede: ");
            String nome = readNonEmptyLine(sc);

            if (findHospedeByDocumento(doc) != null) {
                System.out.println("Falha: documento duplicado.");
                return;
            }

            h = new Hospede(nextHospedeId++, nome, doc);
            hospedes[nHospedes++] = h;
            System.out.println("Hóspede criado: ID " + h.id);
        }

        System.out.print("Número de hóspedes (1..capacidade): ");
        int numHosp = readInt(sc);

        LocalDate inicio = readDate(sc, "Data início (YYYY-MM-DD): ");
        LocalDate fim = readDate(sc, "Data fim (YYYY-MM-DD): ");

        if (inicio.isAfter(fim)) {
            System.out.println("Falha:  deve ser menor que "+ inicio);
            return;
        }

        Quarto melhor = null;
        for (int i = 0; i < nQuartos; i++) {
            Quarto q = quartos[i];
            if (q.capacidade < numHosp)
                continue;
            if (temConflitoReservaAtiva(q.id, -1, inicio, fim))
                continue;

            if (melhor == null)
                melhor = q;
            else if (q.capacidade < melhor.capacidade)
                melhor = q;
            else if (q.capacidade == melhor.capacidade && q.numero < melhor.numero)
                melhor = q;
        }

        if (melhor == null) {
            System.out.println("Falha: não existe quarto disponível adequado para essas datas.");
            return;
        }

        Reserva r = new Reserva(nextReservaId++, melhor.id, h.id, numHosp, inicio, fim, true);
        reservas[nReservas++] = r;

        atualizarOcupacaoComBaseNasReservas();

        System.out.println("Reserva criada com sucesso!");
        System.out.println("Reserva ID: " + r.id + " | Quarto Nº " + melhor.numero + " | Hóspede: " + h.nome);
    }

    private static void listarTodasReservas() {
        if (nReservas == 0) {
            System.out.println("Sem reservas.");
            return;
        }
        System.out.println("\nID | Quarto | Hóspede | NºHosp | Início | Fim | Ativa");
        for (int i = 0; i < nReservas; i++) {
            Reserva r = reservas[i];
            Quarto q = findQuartoById(r.idQuarto);
            Hospede h = findHospedeById(r.idHospede);
            System.out.printf("%d | %s | %s | %d | %s | %s | %s%n",
                    r.id,
                    (q != null ? String.valueOf(q.numero) : "N/A"),
                    (h != null ? h.nome : "N/A"),
                    r.numeroHospedes,
                    r.dataInicio, r.dataFim,
                    r.ativa ? "SIM" : "NÃO");
        }
    }

    private static void listarReservasPorQuarto(Scanner sc) {
        System.out.print("ID do quarto: ");
        int idQ = readInt(sc);
        Quarto q = findQuartoById(idQ);
        if (q == null) {
            System.out.println("Quarto não encontrado.");
            return;
        }

        LocalDate hoje = LocalDate.now();
        System.out.println("Reservas presentes ou futuras (ativas) do Quarto Nº " + q.numero + " (hoje=" + hoje + "):");

        boolean encontrou = false;
        for (int i = 0; i < nReservas; i++) {
            Reserva r = reservas[i];
            if (!r.ativa)
                continue;
            if (r.idQuarto != idQ)
                continue;

            boolean presente = !hoje.isBefore(r.dataInicio) && !hoje.isAfter(r.dataFim);
            boolean futura = r.dataInicio.isAfter(hoje);

            if (presente || futura) {
                Hospede h = findHospedeById(r.idHospede);
                System.out.printf("Reserva %d | %s | %d hóspedes | %s a %s | %s%n",
                        r.id,
                        (h != null ? h.nome + " [" + h.documento + "]" : "N/A"),
                        r.numeroHospedes,
                        r.dataInicio, r.dataFim,
                        presente ? "PRESENTE" : "FUTURA");
                encontrou = true;
            }
        }
        if (!encontrou)
            System.out.println("(nenhuma)");
    }

    private static void listarReservasPorHospede(Scanner sc) {
        System.out.print("Documento do hóspede: ");
        String doc = readNonEmptyLine(sc);
        Hospede h = findHospedeByDocumento(doc);
        if (h == null) {
            System.out.println("Hóspede não encontrado.");
            return;
        }

        LocalDate hoje = LocalDate.now();
        System.out.println("Reservas presentes ou futuras (ativas) de " + h.nome + " (hoje=" + hoje + "):");

        boolean encontrou = false;
        for (int i = 0; i < nReservas; i++) {
            Reserva r = reservas[i];
            if (!r.ativa)
                continue;
            if (r.idHospede != h.id)
                continue;

            boolean presente = !hoje.isBefore(r.dataInicio) && !hoje.isAfter(r.dataFim);
            boolean futura = r.dataInicio.isAfter(hoje);

            if (presente || futura) {
                Quarto q = findQuartoById(r.idQuarto);
                System.out.printf("Reserva %d | Quarto %s | %d hóspedes | %s a %s | %s%n",
                        r.id,
                        (q != null ? String.valueOf(q.numero) : "N/A"),
                        r.numeroHospedes,
                        r.dataInicio, r.dataFim,
                        presente ? "PRESENTE" : "FUTURA");
                encontrou = true;
            }
        }
        if (!encontrou)
            System.out.println("(nenhuma)");
    }

    private static void editarReserva(Scanner sc) {
        System.out.print("ID da reserva: ");
        int id = readInt(sc);
        Reserva r = findReservaById(id);
        if (r == null) {
            System.out.println("Reserva não encontrada.");
            return;
        }
        if (!r.ativa) {
            System.out.println("Não é possível editar: reserva está cancelada (inativa).");
            return;
        }

        Quarto q = findQuartoById(r.idQuarto);
        Hospede h = findHospedeById(r.idHospede);

        System.out.println("Reserva atual:");
        System.out.printf("ID %d | Quarto %s | Hóspede %s | NºHosp %d | %s a %s | Ativa=%s%n",
                r.id,
                (q != null ? q.numero : "N/A"),
                (h != null ? h.nome : "N/A"),
                r.numeroHospedes,
                r.dataInicio, r.dataFim,
                r.ativa ? "SIM" : "NÃO");

        System.out.print("Novo nº hóspedes (-1 para manter): ");
        int novoNum = readInt(sc);
        if (novoNum == -1)
            novoNum = r.numeroHospedes;

        LocalDate novoInicio = readDateOrKeep(sc, "Nova data início (YYYY-MM-DD) ou ENTER para manter: ", r.dataInicio);
        LocalDate novoFim = readDateOrKeep(sc, "Nova data fim (YYYY-MM-DD) ou ENTER para manter: ", r.dataFim);

        if (novoInicio.isAfter(novoFim)) {
            System.out.println("Falha: dataInicio deve ser <= dataFim.");
            return;
        }

        if (q == null) {
            System.out.println("Falha: quarto da reserva não existe.");
            return;
        }

        if (novoNum < 1 || novoNum > q.capacidade) {
            System.out
                    .println("Falha: nº hóspedes deve estar entre 1 e a capacidade do quarto (" + q.capacidade + ").");
            return;
        }

        if (temConflitoReservaAtiva(r.idQuarto, r.id, novoInicio, novoFim)) {
            System.out.println("Falha: conflito de datas com outra reserva ativa no mesmo quarto.");
            return;
        }

        r.numeroHospedes = novoNum;
        r.dataInicio = novoInicio;
        r.dataFim = novoFim;

        atualizarOcupacaoComBaseNasReservas();
        System.out.println("Reserva atualizada com sucesso.");
    }

    private static void cancelarReserva(Scanner sc) {
        System.out.print("ID da reserva a cancelar: ");
        int id = readInt(sc);
        Reserva r = findReservaById(id);
        if (r == null) {
            System.out.println("Reserva não encontrada.");
            return;
        }
        if (!r.ativa) {
            System.out.println("Reserva já está cancelada.");
            return;
        }
        r.ativa = false;
        atualizarOcupacaoComBaseNasReservas();
        System.out.println("Reserva cancelada com sucesso.");
    }

    // ===================== REGRAS / VALIDAÇÕES =====================

    private static boolean intervalosIntersectam(LocalDate aIni, LocalDate aFim, LocalDate bIni, LocalDate bFim) {
        return !aIni.isAfter(bFim) && !bIni.isAfter(aFim);
    }

    private static boolean temConflitoReservaAtiva(int idQuarto, int excluirIdReserva, LocalDate ini, LocalDate fim) {
        for (int i = 0; i < nReservas; i++) {
            Reserva r = reservas[i];
            if (!r.ativa)
                continue;
            if (r.idQuarto != idQuarto)
                continue;
            if (r.id == excluirIdReserva)
                continue;

            if (intervalosIntersectam(r.dataInicio, r.dataFim, ini, fim))
                return true;
        }
        return false;
    }

    private static void atualizarOcupacaoComBaseNasReservas() {
        LocalDate hoje = LocalDate.now();

        for (int i = 0; i < nQuartos; i++)
            quartos[i].estaOcupado = false;

        for (int i = 0; i < nReservas; i++) {
            Reserva r = reservas[i];
            if (!r.ativa)
                continue;

            boolean presente = !hoje.isBefore(r.dataInicio) && !hoje.isAfter(r.dataFim);
            if (presente) {
                Quarto q = findQuartoById(r.idQuarto);
                if (q != null)
                    q.estaOcupado = true;
            }
        }
    }

    private static Reserva reservaAtivaPresenteDoQuarto(int idQuarto, LocalDate hoje) {
        for (int i = 0; i < nReservas; i++) {
            Reserva r = reservas[i];
            if (!r.ativa)
                continue;
            if (r.idQuarto != idQuarto)
                continue;

            boolean presente = !hoje.isBefore(r.dataInicio) && !hoje.isAfter(r.dataFim);
            if (presente)
                return r;
        }
        return null;
    }

    // ===================== PESQUISAS =====================

    private static Quarto findQuartoById(int id) {
        for (int i = 0; i < nQuartos; i++)
            if (quartos[i].id == id)
                return quartos[i];
        return null;
    }

    private static Hospede findHospedeById(int id) {
        for (int i = 0; i < nHospedes; i++)
            if (hospedes[i].id == id)
                return hospedes[i];
        return null;
    }

    private static Hospede findHospedeByDocumento(String doc) {
        for (int i = 0; i < nHospedes; i++)
            if (hospedes[i].documento.equalsIgnoreCase(doc))
                return hospedes[i];
        return null;
    }

    private static Reserva findReservaById(int id) {
        for (int i = 0; i < nReservas; i++)
            if (reservas[i].id == id)
                return reservas[i];
        return null;
    }

    // ===================== CSV LOAD/SAVE =====================

    private static void loadQuartos(String file) {
        nQuartos = 0;
        File f = new File(file);
        if (!f.exists()) {
            System.out.println("Aviso: " + file + " não existe. Sem quartos.");
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;

                String[] p = line.split(",");
                if (p.length < 3)
                    continue;

                if (!isInt(p[0].trim()))
                    continue; // ignora cabeçalho

                int id = Integer.parseInt(p[0].trim());
                int numero = Integer.parseInt(p[1].trim());
                int capacidade = Integer.parseInt(p[2].trim());

                if (nQuartos >= MAX_QUARTOS)
                    break;

                quartos[nQuartos++] = new Quarto(id, numero, capacidade, false);
            }
            System.out.println("Quartos carregados: " + nQuartos);
        } catch (IOException e) {
            System.out.println("Erro ao ler " + file + ": " + e.getMessage());
        }
    }

    private static void loadHospedes(String file) {
        nHospedes = 0;
        nextHospedeId = 1;

        File f = new File(file);
        if (!f.exists()) {
            System.out.println("Aviso: " + file + " não existe. Sem hóspedes.");
            return;
        }

        int maxId = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;

                String[] p = line.split(",");
                if (p.length < 3)
                    continue;

                if (!isInt(p[0].trim()))
                    continue; // ignora cabeçalho

                int id = Integer.parseInt(p[0].trim());
                String nome = p[1].trim();
                String doc = p[2].trim();

                if (nome.isEmpty() || doc.isEmpty())
                    continue;
                if (findHospedeByDocumento(doc) != null)
                    continue;
                if (nHospedes >= MAX_HOSPEDES)
                    break;

                hospedes[nHospedes++] = new Hospede(id, nome, doc);
                if (id > maxId)
                    maxId = id;
            }

            nextHospedeId = maxId + 1;
            System.out.println("Hóspedes carregados: " + nHospedes);
        } catch (IOException e) {
            System.out.println("Erro ao ler " + file + ": " + e.getMessage());
        }
    }

    private static void loadReservas(String file) {
        nReservas = 0;
        nextReservaId = 1;

        File f = new File(file);
        if (!f.exists()) {
            System.out.println("Aviso: " + file + " não existe. Sem reservas.");
            return;
        }

        int maxId = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;

                String[] p = line.split(",");
                if (p.length < 7)
                    continue;

                if (!isInt(p[0].trim()))
                    continue; // ignora cabeçalho

                int id = Integer.parseInt(p[0].trim());
                int idQuarto = Integer.parseInt(p[1].trim());
                int idHospede = Integer.parseInt(p[2].trim());
                int numHosp = Integer.parseInt(p[3].trim());
                LocalDate ini = LocalDate.parse(p[4].trim());
                LocalDate fim = LocalDate.parse(p[5].trim());
                boolean ativa = Boolean.parseBoolean(p[6].trim());

                if (ini.isAfter(fim))
                    continue;
                if (numHosp < 1)
                    continue;
                if (nReservas >= MAX_RESERVAS)
                    break;

                reservas[nReservas++] = new Reserva(id, idQuarto, idHospede, numHosp, ini, fim, ativa);
                if (id > maxId)
                    maxId = id;
            }

            nextReservaId = maxId + 1;
            System.out.println("Reservas carregadas: " + nReservas);
        } catch (IOException | DateTimeParseException e) {
            System.out.println("Erro ao ler " + file + ": " + e.getMessage());
        }
    }

    private static void saveHospedes(String file) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("id,nome,documento");
            for (int i = 0; i < nHospedes; i++) {
                Hospede h = hospedes[i];
                pw.printf("%d,%s,%s%n", h.id, sanitizeCsv(h.nome), sanitizeCsv(h.documento));
            }
        } catch (IOException e) {
            System.out.println("Erro ao guardar hóspedes: " + e.getMessage());
        }
    }

    private static void saveReservas(String file) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println("id,idQuarto,idHospede,numeroHospedes,dataInicio,dataFim,ativa");
            for (int i = 0; i < nReservas; i++) {
                Reserva r = reservas[i];
                pw.printf("%d,%d,%d,%d,%s,%s,%s%n",
                        r.id, r.idQuarto, r.idHospede, r.numeroHospedes,
                        r.dataInicio, r.dataFim, r.ativa);
            }
        } catch (IOException e) {
            System.out.println("Erro ao guardar reservas: " + e.getMessage());
        }
    }

    private static String sanitizeCsv(String s) {
        return s.replace(",", " ");
    }

    // ===================== INPUT HELPERS =====================

    private static int readInt(Scanner sc) {
        while (true) {
            String s = sc.nextLine().trim();
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.print("Valor inválido. Tente novamente: ");
            }
        }
    }

    private static String readNonEmptyLine(Scanner sc) {
        while (true) {
            String s = sc.nextLine().trim();
            if (!s.isEmpty())
                return s;
            System.out.print("Não pode ser vazio. Tente novamente: ");
        }
    }

    private static LocalDate readDate(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            try {
                return LocalDate.parse(s);
            } catch (DateTimeParseException e) {
                System.out.println("Data inválida. Use YYYY-MM-DD.");
            }
        }
    }

    private static LocalDate readDateOrKeep(Scanner sc, String prompt, LocalDate keep) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            if (s.isEmpty())
                return keep;
            try {
                return LocalDate.parse(s);
            } catch (DateTimeParseException e) {
                System.out.println("Data inválida. Use YYYY-MM-DD.");
            }
        }
    }

    private static boolean isInt(String s) {
        try {
            Integer.parseInt(s.trim());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
