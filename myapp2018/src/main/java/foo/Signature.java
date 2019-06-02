package foo;

public class Signature {
	public String title;
	public String signatory;

	public Boolean testValid() {
		return this.title != null && this.signatory != null ;
	}
}
