package de.sybig.oba.server;

public abstract class JsonEntity {

    /**
     * To avoid a recursive retrieval of the complete ontology. The parents and
     * children of a class are not filled with their parents and children. These
     * not completely filled classes are seen as shells.
     */
    protected boolean shell = true;
    protected String name;
    protected String namespace;

    /**
     * If the class is not completely filled with parents and children "shell"
     * is set to
     * <code>true</code>.
     *
     * @return
     * <code>false</code> if the class is filled completely,
     * <code>true</code> if the object is just a shell for content.
     */
    public boolean isShell() {
        return shell;
    }

    /**
     * Sets whether this object is a shell and so the parents and children are
     * not completely filled.
     *
     * @param shell the shell to set
     */
    public void setShell(boolean shell) {
        this.shell = shell;
    }

    /**
     * Get the name of class (not the label)
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param Sets the name of the class. (not the label) the name to set to the
     * complete name, including the namespace.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the namespace of the the class.
     *
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Sets the namespace of the class
     *
     * @param namespace the namespace to set
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result
                + ((namespace == null) ? 0 : namespace.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof JsonEntity)) {
            return false;
        }
        JsonEntity other = (JsonEntity) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (namespace == null) {
            if (other.namespace != null) {
                return false;
            }
        } else if (!namespace.equals(other.namespace)) {
            return false;
        }
        return true;
    }
}
