import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// Exceções Personalizadas
class EventoLotadoException extends Exception {
    public EventoLotadoException(String message) {
        super(message);
    }
}

class ParticipanteDuplicadoException extends Exception {
    public ParticipanteDuplicadoException(String message) {
        super(message);
    }
}

// Classe Participante
class Participante {
    private String nome;
    private String email;
    private String tipo;

    public Participante(String nome, String email, String tipo) {
        this.nome = nome;
        this.email = email;
        this.tipo = tipo;
    }

    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getTipo() { return tipo; }

    @Override
    public String toString() {
        return "Nome: " + nome + ", Email: " + email + ", Tipo: " + tipo;
    }
}

// Classe Base Evento
abstract class Evento {
    private String nome;
    private String data;
    private String local;
    private int capacidadeMaxima;
    private List<Participante> participantes = new ArrayList<>();

    public Evento(String nome, String data, String local, int capacidadeMaxima) {
        this.nome = nome;
        this.data = data;
        this.local = local;
        this.capacidadeMaxima = capacidadeMaxima;
    }

    public void adicionarParticipante(Participante participante) throws EventoLotadoException, ParticipanteDuplicadoException {
        if (participantes.size() >= capacidadeMaxima) {
            throw new EventoLotadoException("Evento já está lotado.");
        }
        if (participantes.contains(participante)) {
            throw new ParticipanteDuplicadoException("Participante já registrado no evento.");
        }
        participantes.add(participante);
    }

    public String getNome() { return nome; }
    public String getData() { return data; }
    public String getLocal() { return local; }
    public int getCapacidadeMaxima() { return capacidadeMaxima; }
    public List<Participante> getParticipantes() { return participantes; }

    // Método abstrato para salvar o evento no banco de dados
    public abstract void salvar(Connection conn) throws SQLException;

    @Override
    public String toString() {
        return "Nome: " + nome + ", Data: " + data + ", Local: " + local + ", Capacidade: " + capacidadeMaxima;
    }
}

// Classe Palestra
class Palestra extends Evento {
    private String palestrante;
    private String temas;
    private int duracao;

    public Palestra(String nome, String data, String local, int capacidadeMaxima, String palestrante, String temas, int duracao) {
        super(nome, data, local, capacidadeMaxima);
        this.palestrante = palestrante;
        this.temas = temas;
        this.duracao = duracao;
    }

    public void salvar(Connection conn) throws SQLException {
        String sql = "INSERT INTO Evento (nome, data, local, capacidade_maxima, tipo) VALUES (?, ?, ?, ?, 'Palestra')";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, getNome());
            pstmt.setString(2, getData());
            pstmt.setString(3, getLocal());
            pstmt.setInt(4, getCapacidadeMaxima());
            pstmt.executeUpdate();
        }
    }
}

// Classe Workshop
class Workshop extends Evento {
    private String instrutor;
    private String materiais;
    private int cargaHoraria;

    public Workshop(String nome, String data, String local, int capacidadeMaxima, String instrutor, String materiais, int cargaHoraria) {
        super(nome, data, local, capacidadeMaxima);
        this.instrutor = instrutor;
        this.materiais = materiais;
        this.cargaHoraria = cargaHoraria;
    }

    public void salvar(Connection conn) throws SQLException {
        String sql = "INSERT INTO Evento (nome, data, local, capacidade_maxima, tipo) VALUES (?, ?, ?, ?, 'Workshop')";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, getNome());
            pstmt.setString(2, getData());
            pstmt.setString(3, getLocal());
            pstmt.setInt(4, getCapacidadeMaxima());
            pstmt.executeUpdate();
        }
    }
}

// Classe de Conexão e Teste
public class SistemaEventos {
    private static Connection conectar() throws SQLException {
        String url = "jdbc:sqlite:eventos.db";
        return DriverManager.getConnection(url);
    }

    public static void listarEventos(Connection conn) throws SQLException {
        String sql = "SELECT * FROM Evento";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") + ", Nome: " + rs.getString("nome") + ", Data: " + rs.getString("data") +
                                   ", Local: " + rs.getString("local") + ", Capacidade: " + rs.getInt("capacidade_maxima") +
                                   ", Tipo: " + rs.getString("tipo"));
            }
        }
    }

    public static void adicionarParticipante(Connection conn, String nome, String email, String tipo, int eventoId) throws SQLException, ParticipanteDuplicadoException {
        String sql = "INSERT INTO Participante (nome, email, tipo, evento_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nome);
            pstmt.setString(2, email);
            pstmt.setString(3, tipo);
            pstmt.setInt(4, eventoId);
            pstmt.executeUpdate();
        }
    }

    private static void mostrarParticipantes(Connection conn, String nomeEvento) throws SQLException {
        String sql = "SELECT p.nome, p.tipo FROM Participante p " +
                    "JOIN Evento e ON p.evento_id = e.id " +
                    "WHERE e.nome = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nomeEvento);
            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.println("Participantes do evento: " + nomeEvento);
                while (rs.next()) {
                    System.out.println("- " + rs.getString("nome") + " (" + rs.getString("tipo") + ")");
                }
            }
        }
    }

    public static void gerarRelatorioEventos(Connection conn) throws SQLException {
        String sql = "SELECT e.nome, " +
                    "SUM(CASE WHEN p.tipo = 'VIP' THEN 1 ELSE 0 END) AS total_vip, " +
                    "SUM(CASE WHEN p.tipo = 'Normal' THEN 1 ELSE 0 END) AS total_normal " +
                    "FROM Evento e " +
                    "LEFT JOIN Participante p ON e.id = p.evento_id " +
                    "GROUP BY e.id";
        
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                System.out.println("Evento: " + rs.getString("nome") + 
                                ", Total VIP: " + rs.getInt("total_vip") + 
                                ", Total NORMAL: " + rs.getInt("total_normal"));
            }
        }
    }

    public static void gerarRelatorioEvento(Connection conn, String nomeEvento) throws SQLException {
        String sql = "SELECT e.nome, " +
                    "SUM(CASE WHEN p.tipo = 'VIP' THEN 1 ELSE 0 END) AS total_vip, " +
                    "SUM(CASE WHEN p.tipo = 'Normal' THEN 1 ELSE 0 END) AS total_normal " +
                    "FROM Evento e " +
                    "LEFT JOIN Participante p ON e.id = p.evento_id " +
                    "WHERE e.nome = ? " +
                    "GROUP BY e.id";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nomeEvento);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Evento: " + rs.getString("nome") +
                                    ", Total VIP: " + rs.getInt("total_vip") +
                                    ", Total Normal: " + rs.getInt("total_normal"));
                    mostrarParticipantes(conn, nomeEvento);
                } else {
                    System.out.println("Nenhum evento encontrado com o nome: " + nomeEvento);
                }
            }
        }
    }

    public static void removerParticipantePorNome(Connection conn, String nomeParticipante, String nomeEventoParticipante) throws SQLException {
        String sql = "DELETE FROM Participante " +
                 "WHERE nome = ? AND evento_id = (SELECT id FROM Evento WHERE nome = ?)";        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nomeParticipante);
            pstmt.setString(2, nomeEventoParticipante);
            int linhasAfetadas = pstmt.executeUpdate();
            
            if (linhasAfetadas > 0) {
                System.out.println("Participante removido da reserva: " + nomeParticipante);
            } else {
                System.out.println("Nenhum participante encontrado com o nome: " + nomeParticipante);
            }
        }
    }

    public static void main(String[] args) {
        try (Connection conn = conectar(); Scanner scanner = new Scanner(System.in)) {
            // Criação das tabelas
            String sqlEvento = "CREATE TABLE IF NOT EXISTS Evento (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "nome TEXT," +
                    "data TEXT," +
                    "local TEXT," +
                    "capacidade_maxima INTEGER," +
                    "tipo TEXT)";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sqlEvento);
            }

            String sqlParticipante = "CREATE TABLE IF NOT EXISTS Participante (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "nome TEXT," +
                    "email TEXT," +
                    "tipo TEXT," +
                    "evento_id INTEGER," +
                    "FOREIGN KEY(evento_id) REFERENCES Evento(id))";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sqlParticipante);
            }

            boolean running = true;

            while (running) {
                System.out.println("Menu:");
                System.out.println("1. Adicionar Palestra");
                System.out.println("2. Adicionar Workshop");
                System.out.println("3. Listar Eventos");
                System.out.println("4. Cadastrar Reserva em Evento");
                System.out.println("5. Gerar Relatório de Eventos");
                System.out.println("6. Gerar Relatório de Evento");
                System.out.println("7. Cancelar Reserva");
                System.out.println("8. Sair");
                System.out.print("Escolha uma opção: ");

                int opcao = scanner.nextInt();
                scanner.nextLine(); 

                switch (opcao) {
                    case 1:
                        System.out.print("Nome da Palestra: ");
                        String nomePalestra = scanner.nextLine();
                        System.out.print("Data (YYYY-MM-DD): ");
                        String dataPalestra = scanner.nextLine();
                        System.out.print("Local: ");
                        String localPalestra = scanner.nextLine();
                        System.out.print("Capacidade Máxima: ");
                        int capacidadePalestra = scanner.nextInt();
                        scanner.nextLine();
                        System.out.print("Palestrante: ");
                        String palestrante = scanner.nextLine();
                        System.out.print("Temas: ");
                        String temas = scanner.nextLine();
                        System.out.print("Duração (minutos): ");
                        int duracao = scanner.nextInt();

                        Palestra palestra = new Palestra(nomePalestra, dataPalestra, localPalestra, capacidadePalestra, palestrante, temas, duracao);
                        palestra.salvar(conn);
                        System.out.println("Palestra adicionada com sucesso.");
                        break;

                    case 2:
                        System.out.print("Nome do Workshop: ");
                        String nomeWorkshop = scanner.nextLine();
                        System.out.print("Data (YYYY-MM-DD): ");
                        String dataWorkshop = scanner.nextLine();
                        System.out.print("Local: ");
                        String localWorkshop = scanner.nextLine();
                        System.out.print("Capacidade Máxima: ");
                        int capacidadeWorkshop = scanner.nextInt();
                        scanner.nextLine(); 
                        System.out.print("Instrutor: ");
                        String instrutor = scanner.nextLine();
                        System.out.print("Materiais: ");
                        String materiais = scanner.nextLine();
                        System.out.print("Carga Horária (horas): ");
                        int cargaHoraria = scanner.nextInt();

                        Workshop workshop = new Workshop(nomeWorkshop, dataWorkshop, localWorkshop, capacidadeWorkshop, instrutor, materiais, cargaHoraria);
                        workshop.salvar(conn);
                        System.out.println("Workshop adicionado com sucesso.");
                        break;

                    case 3:
                        System.out.println("Eventos Cadastrados:");
                        listarEventos(conn);
                        break;

                    case 4:
                        System.out.print("ID do Evento: ");
                        int eventoId = scanner.nextInt();
                        scanner.nextLine(); 
                        System.out.print("Nome do Participante: ");
                        String nomeParticipante = scanner.nextLine();
                        System.out.print("Email do Participante: ");
                        String emailParticipante = scanner.nextLine();
                        System.out.print("Tipo (Normal ou VIP): ");
                        String tipoParticipante = "";

                        while (true) {
                            tipoParticipante = scanner.nextLine().trim();

                            if (tipoParticipante.equalsIgnoreCase("Normal") || tipoParticipante.equalsIgnoreCase("VIP")) {
                                break;
                            } else {
                                System.out.println("Entrada inválida. Por favor, digite 'Normal' ou 'VIP'.");
                            }
                        }
                        

                        try {
                            adicionarParticipante(conn, nomeParticipante, emailParticipante, tipoParticipante, eventoId);
                            System.out.println("Participante adicionado com sucesso a reserva.");
                        } catch (SQLException | ParticipanteDuplicadoException e) {
                            System.out.println(e.getMessage());
                        }
                        break;

                    case 5:
                        System.out.println("Relatório de Eventos:");
                        gerarRelatorioEventos(conn);
                        break;

                    case 6:
                        System.out.print("Nome do Evento: ");
                        String nomeEvento = scanner.nextLine();
                        System.out.println("Relatório de Evento:");
                        gerarRelatorioEvento(conn, nomeEvento);
                        break;

                    case 7:
                        System.out.print("Nome do Participante: ");
                        String nomeParticipanteEvento = scanner.nextLine();
                        System.out.print("Nome do Evento: ");
                        String nomeEventoParticipante = scanner.nextLine();
                        removerParticipantePorNome(conn, nomeParticipanteEvento, nomeEventoParticipante);
                        break;

                    case 8:
                        running = false;
                        break;

                    default:
                        System.out.println("Opção inválida. Tente novamente.");
                }
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
