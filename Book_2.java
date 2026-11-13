import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

// Interface
interface Borrowable {
    boolean borrowItem();
    boolean returnItem();
}

// Custom Exception
class ItemNotFoundException extends Exception {
    public ItemNotFoundException(String message) {
        super(message);
    }
}

// Abstract parent class
abstract class LibraryItem implements Borrowable {
    private String id;
    private String title;
    private boolean borrowed;

    public LibraryItem(String id, String title, boolean borrowed) {
        this.id = id;
        this.title = title;
        this.borrowed = borrowed;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public boolean isBorrowed() {
        return borrowed;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setBorrowed(boolean borrowed) {
        this.borrowed = borrowed;
    }

    public abstract String getDetails();
}

// Book child class
class Book extends LibraryItem {
    public static final String CATEGORY = "BOOK";
    private String author;

    public Book(String id, String title, String author, boolean borrowed) {
        super(id, title, borrowed);
        this.author = author;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    @Override
    public boolean borrowItem() {
        if (isBorrowed()) {
            return false;
        }
        setBorrowed(true);
        return true;
    }

    @Override
    public boolean returnItem() {
        if (!isBorrowed()) {
            return false;
        }
        setBorrowed(false);
        return true;
    }

    @Override
    public String getDetails() {
        return getId() + " | " + getTitle() + " | " + author + " | "
                + (isBorrowed() ? "Borrowed" : "Available");
    }
}

// Main GUI class.
public class Book_2 extends JFrame {

    private static final String DB_URL = "jdbc:sqlite:library.db";

    private final List<LibraryItem> catalog = new ArrayList<>();
    private Connection connection;

    private DefaultTableModel model;
    private JTable table;
    private JTextField searchField;
    private JLabel totalLabel;
    private JLabel availableLabel;
    private JLabel borrowedLabel;
    private TableRowSorter<DefaultTableModel> sorter;

    public Book_2() {
        super("School Library Management System");
        setTitle("School Library Management System");
        setSize(1050, 680);
        setMinimumSize(new java.awt.Dimension(850, 580));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(240, 244, 248));

        createHeaderAndDashboard();
        createTable();
        createButtons();

        connectToDatabase();
        if (connection != null) {
            loadBooksFromDatabase();
        }
        refreshTable();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitSystem();
            }
        });
    }

    // Connect Java to the SQLite database.
    private void connectToDatabase() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            initializeDatabase();
            System.out.println("Connected to SQLite successfully.");
        } catch (SQLException ex) {
            showError("Cannot connect to SQLite.\n"
                    + "Check that the library.db file is accessible and writable.\n\n"
                    + ex.getMessage());
        }
    }

    private void initializeDatabase() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS books ("
                    + "book_id TEXT PRIMARY KEY, "
                    + "title TEXT NOT NULL, "
                    + "author TEXT NOT NULL, "
                    + "status TEXT DEFAULT 'Available', "
                    + "category TEXT DEFAULT 'BOOK')");

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM books");
            if (rs.next() && rs.getInt(1) == 0) {
                String[] seedBooks = {
                    "INSERT INTO books (book_id, title, author, status, category) VALUES ('B001', 'The Hobbit', 'J.R.R. Tolkien', 'Available', 'Fantasy')",
                    "INSERT INTO books (book_id, title, author, status, category) VALUES ('B002', 'Harry Potter and the Sorcerer''s Stone', 'J.K. Rowling', 'Available', 'Fantasy')",
                    "INSERT INTO books (book_id, title, author, status, category) VALUES ('B003', 'Wonder', 'R.J. Palacio', 'Borrowed', 'Fiction')",
                    "INSERT INTO books (book_id, title, author, status, category) VALUES ('B004', 'The Little Prince', 'Antoine de Saint-Exupery', 'Available', 'Fiction')",
                    "INSERT INTO books (book_id, title, author, status, category) VALUES ('B005', 'Noli Me Tangere', 'Jose Rizal', 'Available', 'Classic')",
                    "INSERT INTO books (book_id, title, author, status, category) VALUES ('B006', 'El Filibusterismo', 'Jose Rizal', 'Available', 'Classic')",
                    "INSERT INTO books (book_id, title, author, status, category) VALUES ('B007', 'Charlotte''s Web', 'E.B. White', 'Available', 'Children')",
                    "INSERT INTO books (book_id, title, author, status, category) VALUES ('B008', 'The Giver', 'Lois Lowry', 'Available', 'Science Fiction')",
                    "INSERT INTO books (book_id, title, author, status, category) VALUES ('B009', 'Percy Jackson and the Lightning Thief', 'Rick Riordan', 'Available', 'Fantasy')",
                    "INSERT INTO books (book_id, title, author, status, category) VALUES ('B010', 'The Alchemist', 'Paulo Coelho', 'Available', 'Fiction')"
                };

                for (String sql : seedBooks) {
                    stmt.executeUpdate(sql);
                }
            }
            rs.close();
        }
    }

    // Read all books from SQLite.
    private void loadBooksFromDatabase() {
        catalog.clear();

        if (connection == null) {
            return;
        }

        String sql = "SELECT book_id, title, author, status FROM books";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String status = rs.getString("status");
                boolean borrowed = status != null
                        && status.equalsIgnoreCase("Borrowed");

                catalog.add(new Book(
                        rs.getString("book_id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        borrowed
                ));
            }
        } catch (SQLException ex) {
            showError("Unable to load books:\n" + ex.getMessage());
        }
    }

    // Run INSERT, UPDATE, or DELETE statements safely.
    private int executeDatabaseUpdate(String sql, String... values) {
        if (connection == null) {
            showError("There is no database connection. Check your MySQL settings.");
            return -1;
        }

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) {
                ps.setString(i + 1, values[i]);
            }
            return ps.executeUpdate();
        } catch (SQLException ex) {
            showError("Database operation failed:\n" + ex.getMessage());
            return -1;
        }
    }

    // Header and dashboard share one top panel to avoid BorderLayout overlap.
    private void createHeaderAndDashboard() {
        JPanel topPanel = new JPanel(new BorderLayout(0, 10));
        topPanel.setBackground(new Color(240, 244, 248));

        JLabel heading = new JLabel(
                "SCHOOL LIBRARY MANAGEMENT SYSTEM",
                SwingConstants.CENTER
        );
        heading.setFont(new Font("Arial", Font.BOLD, 25));
        heading.setForeground(Color.WHITE);
        heading.setOpaque(true);
        heading.setBackground(new Color(30, 65, 110));
        heading.setBorder(BorderFactory.createEmptyBorder(22, 10, 22, 10));
        topPanel.add(heading, BorderLayout.NORTH);

        JPanel dashboard = new JPanel(new GridLayout(1, 3, 15, 0));
        dashboard.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));
        dashboard.setBackground(new Color(240, 244, 248));

        totalLabel = createDashboardLabel("Total Books: 0",
                new Color(45, 100, 160));
        availableLabel = createDashboardLabel("Available: 0",
                new Color(40, 140, 85));
        borrowedLabel = createDashboardLabel("Borrowed: 0",
                new Color(190, 100, 40));

        dashboard.add(totalLabel);
        dashboard.add(availableLabel);
        dashboard.add(borrowedLabel);

        topPanel.add(dashboard, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);
    }

    private JLabel createDashboardLabel(String text, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        label.setForeground(Color.WHITE);
        label.setOpaque(true);
        label.setBackground(color);
        label.setBorder(BorderFactory.createEmptyBorder(16, 5, 16, 5));
        return label;
    }

    // Create the book table.
    private void createTable() {
        String[] columns = {
            "Book ID", "Title", "Author", "Status", "Category"
        };

        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setRowHeight(30);
        table.setFont(new Font("Arial", Font.PLAIN, 13));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        table.getTableHeader().setBackground(new Color(215, 225, 238));
        table.setSelectionBackground(new Color(190, 215, 240));

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 20));
        add(scrollPane, BorderLayout.CENTER);
    }

    // Create search field and action buttons.
    private void createButtons() {
        JPanel bottom = new JPanel(new BorderLayout(8, 10));
        bottom.setBackground(new Color(240, 244, 248));
        bottom.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBackground(new Color(240, 244, 248));

        searchField = new JTextField(22);
        JButton searchButton = new JButton("Search");
        JButton viewAllButton = new JButton("View All");

        searchPanel.add(new JLabel("Search Books:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(viewAllButton);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 4, 10, 10));
        buttonPanel.setBackground(new Color(240, 244, 248));

        JButton addButton = new JButton("Add Book");
        JButton editButton = new JButton("Edit Book");
        JButton deleteButton = new JButton("Delete Book");
        JButton borrowButton = new JButton("Borrow Book");
        JButton returnButton = new JButton("Return Book");
        JButton statsButton = new JButton("Statistics");
        JButton modulesButton = new JButton("Modules");
        JButton exitButton = new JButton("Exit");

        styleButton(addButton, new Color(40, 130, 80));
        styleButton(editButton, new Color(45, 105, 170));
        styleButton(deleteButton, new Color(190, 65, 65));
        styleButton(borrowButton, new Color(35, 125, 85));
        styleButton(returnButton, new Color(45, 105, 170));
        styleButton(statsButton, new Color(110, 85, 160));
        styleButton(modulesButton, new Color(90, 100, 115));
        styleButton(exitButton, new Color(80, 80, 80));

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(borrowButton);
        buttonPanel.add(returnButton);
        buttonPanel.add(statsButton);
        buttonPanel.add(modulesButton);
        buttonPanel.add(exitButton);

        bottom.add(searchPanel, BorderLayout.NORTH);
        bottom.add(buttonPanel, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        searchButton.addActionListener(e -> searchBooks());
        searchField.addActionListener(e -> searchBooks());

        viewAllButton.addActionListener(e -> {
            searchField.setText("");
            sorter.setRowFilter(null);
        });

        addButton.addActionListener(e -> addBook());
        editButton.addActionListener(e -> editBook());
        deleteButton.addActionListener(e -> deleteBook());
        borrowButton.addActionListener(e -> processBorrow());
        returnButton.addActionListener(e -> processReturn());
        statsButton.addActionListener(e -> showStatistics());
        modulesButton.addActionListener(e -> showModules());
        exitButton.addActionListener(e -> exitSystem());
    }

    private void styleButton(JButton button, Color color) {
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
        button.setOpaque(true);
    }

    // Refresh table and dashboard using the current database records.
    private void refreshTable() {
        model.setRowCount(0);

        for (LibraryItem item : catalog) {
            Book book = (Book) item;
            model.addRow(new Object[] {
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.isBorrowed() ? "Borrowed" : "Available",
                Book.CATEGORY
            });
        }

        updateDashboard();
    }

    private void updateDashboard() {
        int total = catalog.size();
        int borrowed = 0;

        for (LibraryItem item : catalog) {
            if (item.isBorrowed()) {
                borrowed++;
            }
        }

        totalLabel.setText("Total Books: " + total);
        availableLabel.setText("Available: " + (total - borrowed));
        borrowedLabel.setText("Borrowed: " + borrowed);
    }

    private LibraryItem findItem(String id) throws ItemNotFoundException {
        for (LibraryItem item : catalog) {
            if (item.getId().equalsIgnoreCase(id)) {
                return item;
            }
        }
        throw new ItemNotFoundException("Book not found: " + id);
    }

    // Get selected book, taking table sorting/filtering into account.
    private LibraryItem getSelectedBook() {
        int selectedRow = table.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book first.");
            return null;
        }

        int modelRow = table.convertRowIndexToModel(selectedRow);
        String id = model.getValueAt(modelRow, 0).toString();

        try {
            return findItem(id);
        } catch (ItemNotFoundException ex) {
            showError(ex.getMessage());
            return null;
        }
    }

    // Add a new book to MySQL.
    private void addBook() {
        if (connection == null) {
            showError("Connect to MySQL before adding books.");
            return;
        }

        JTextField idField = new JTextField();
        JTextField titleField = new JTextField();
        JTextField authorField = new JTextField();

        Object[] fields = {
            "Book ID:", idField,
            "Book Title:", titleField,
            "Author:", authorField
        };

        int result = JOptionPane.showConfirmDialog(
                this, fields, "Add New Book", JOptionPane.OK_CANCEL_OPTION);

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String id = idField.getText().trim();
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();

        if (id.isEmpty() || title.isEmpty() || author.isEmpty()) {
            showError("All fields are required.");
            return;
        }

        if (idExists(id)) {
            showError("Book ID already exists.");
            return;
        }

        String sql = "INSERT INTO books "
                + "(book_id, title, author, status, category) "
                + "VALUES (?, ?, ?, 'Available', 'BOOK')";

        int rows = executeDatabaseUpdate(sql, id, title, author);
        if (rows > 0) {
            loadBooksFromDatabase();
            refreshTable();
            showSuccess("Book added successfully.");
        }
    }

    private boolean idExists(String id) {
        for (LibraryItem item : catalog) {
            if (item.getId().equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }

    // Edit the selected book.
    private void editBook() {
        if (connection == null) {
            showError("Connect to MySQL before editing books.");
            return;
        }

        LibraryItem item = getSelectedBook();
        if (item == null) {
            return;
        }

        Book book = (Book) item;
        JTextField idField = new JTextField(book.getId());
        JTextField titleField = new JTextField(book.getTitle());
        JTextField authorField = new JTextField(book.getAuthor());

        Object[] fields = {
            "Book ID:", idField,
            "Book Title:", titleField,
            "Author:", authorField
        };

        int result = JOptionPane.showConfirmDialog(
                this, fields, "Edit Book", JOptionPane.OK_CANCEL_OPTION);

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String newId = idField.getText().trim();
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();

        if (newId.isEmpty() || title.isEmpty() || author.isEmpty()) {
            showError("All fields are required.");
            return;
        }

        if (!newId.equalsIgnoreCase(book.getId()) && idExists(newId)) {
            showError("Book ID already exists.");
            return;
        }

        String sql = "UPDATE books SET book_id=?, title=?, author=? "
                + "WHERE book_id=?";

        int rows = executeDatabaseUpdate(
                sql, newId, title, author, book.getId());

        if (rows > 0) {
            loadBooksFromDatabase();
            refreshTable();
            showSuccess("Book updated successfully.");
        }
    }

    // Delete the selected book.
    private void deleteBook() {
        if (connection == null) {
            showError("Connect to MySQL before deleting books.");
            return;
        }

        LibraryItem item = getSelectedBook();
        if (item == null) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete \"" + item.getTitle() + "\"?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            int rows = executeDatabaseUpdate(
                    "DELETE FROM books WHERE book_id=?", item.getId());

            if (rows > 0) {
                loadBooksFromDatabase();
                refreshTable();
                showSuccess("Book deleted successfully.");
            }
        }
    }

    // Borrow the selected book.
    private void processBorrow() {
        if (connection == null) {
            showError("Connect to MySQL before borrowing books.");
            return;
        }

        LibraryItem item = getSelectedBook();
        if (item == null) {
            return;
        }

        if (item.isBorrowed()) {
            showError("This book is already borrowed.");
            return;
        }

        String sql = "UPDATE books SET status='Borrowed' "
                + "WHERE book_id=? AND status='Available'";

        int rows = executeDatabaseUpdate(sql, item.getId());
        if (rows > 0) {
            loadBooksFromDatabase();
            refreshTable();
            showSuccess("Successfully borrowed: " + item.getTitle());
        } else if (rows == 0) {
            showError("The book status was changed. Refresh and try again.");
        }
    }

    // Return the selected book.
    private void processReturn() {
        if (connection == null) {
            showError("Connect to MySQL before returning books.");
            return;
        }

        LibraryItem item = getSelectedBook();
        if (item == null) {
            return;
        }

        if (!item.isBorrowed()) {
            showError("This book has not been borrowed.");
            return;
        }

        String sql = "UPDATE books SET status='Available' "
                + "WHERE book_id=? AND status='Borrowed'";

        int rows = executeDatabaseUpdate(sql, item.getId());
        if (rows > 0) {
            loadBooksFromDatabase();
            refreshTable();
            showSuccess("Successfully returned: " + item.getTitle());
        } else if (rows == 0) {
            showError("The book status was changed. Refresh and try again.");
        }
    }

    // Search by book ID, title, or author.
    private void searchBooks() {
        String query = searchField.getText().trim();

        if (query.isEmpty()) {
            sorter.setRowFilter(null);
            return;
        }

        sorter.setRowFilter(javax.swing.RowFilter.regexFilter(
                "(?i)" + java.util.regex.Pattern.quote(query), 0, 1, 2));
    }

    private void showStatistics() {
        int total = catalog.size();
        int borrowed = 0;

        for (LibraryItem item : catalog) {
            if (item.isBorrowed()) {
                borrowed++;
            }
        }

        int available = total - borrowed;

        String message = "LIBRARY STATISTICS\n\n"
                + "Total Books: " + total + "\n"
                + "Available Books: " + available + "\n"
                + "Borrowed Books: " + borrowed;

        JOptionPane.showMessageDialog(
                this, message, "Library Statistics",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showModules() {
        String message = "SYSTEM MODULES\n\n"
                + "1. Book Management\n"
                + "2. Search Management\n"
                + "3. Borrowing Management\n"
                + "4. Return Management\n"
                + "5. Library Statistics\n"
                + "6. Data Validation\n"
                + "7. GUI Management\n"
                + "8. SQLite Database Management";

        JOptionPane.showMessageDialog(
                this, message, "System Modules",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showSuccess(String message) {
        JOptionPane.showMessageDialog(
                this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    private void exitSystem() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to exit?",
                "Exit System",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ex) {
                    System.err.println("Error closing database: " + ex.getMessage());
                }
            }
            dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                // Use the default look and feel.
            }

            Book_2 app = new Book_2();
            app.setVisible(true);
        });
    }
}
