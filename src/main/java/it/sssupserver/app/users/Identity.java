package it.sssupserver.app.users;

import it.sssupserver.app.exceptions.*;

public class Identity implements Comparable<Identity> {
    private String username;
    private long id;
    private boolean valid;

    public Identity(String username, long id) {
        this.username = username;
        this.id = id;
        this.valid = true;
    }

    public boolean isValid() {
        return this.valid;
    }

    public void invalidate() {
        this.valid = false;
    }

    public long getId() throws InvalidIdentityException {
        if (!this.isValid()) {
            throw new InvalidIdentityException();
        }
        return this.id;
    }

    public String getUsername() throws InvalidIdentityException {
        if (!this.isValid()) {
            throw new InvalidIdentityException();
        }
        return this.username;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Identity && this.id == ((Identity)obj).id && this.isValid() == ((Identity)obj).isValid();
    }

    @Override
    public int compareTo(Identity o) {
        return this.id < o.id
            ? -1
            : (this.id > o.id
            ? +1
            : 0
            );
    }
}
