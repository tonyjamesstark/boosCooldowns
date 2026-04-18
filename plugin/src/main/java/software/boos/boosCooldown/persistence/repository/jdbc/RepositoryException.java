package software.boos.boosCooldown.persistence.repository.jdbc;

public class RepositoryException extends RuntimeException {

    public RepositoryException(String operation, Throwable cause) {
        super("Repository operation failed: " + operation, cause);
    }
}
