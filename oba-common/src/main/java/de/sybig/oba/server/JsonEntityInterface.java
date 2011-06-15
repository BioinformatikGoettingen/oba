package de.sybig.oba.server;

public interface JsonEntityInterface {

	/**
	 * If the class is not completely filled with parents and children "shell"
	 * is set to <code>true</code>.
	 * 
	 * @return <code>false</code> if the class is filled completely,
	 *         <code>true</code> if the object is just a shell for content.
	 */
	public boolean isShell();

	/**
	 * Sets whether this object is a shell and so the parents and children are
	 * not completely filled.
	 * 
	 * @param shell
	 *            the shell to set
	 */
	public void setShell(boolean shell);

	/**
	 * Get the name of class (not the label)
	 * 
	 * @return the name
	 */
	public String getName();

	/**
	 * @param Sets
	 *            the name of the class. (not the label) the name to set to the
	 *            complete name, including the namespace.
	 */
	public void setName(String name);

	/**
	 * Get the namespace of the the class.
	 * 
	 * @return the namespace
	 */
	public String getNamespace();

	/**
	 * Sets the namespace of the class
	 * 
	 * @param namespace
	 *            the namespace to set
	 */
	public void setNamespace(String namespace);

}