package life.genny.qwandaq;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Answers represents a collection of Answer objects.
 * 
 * @author Adam Crow
 * @author Byron Aguirre
 */
public class Answers implements Serializable {
	
	private static final long serialVersionUID = 1L;
    private List<Answer> answers = new CopyOnWriteArrayList<>();

	public Answers() { }
	
	public Answers(List<Answer> answers) {
		super();
		this.answers = answers;
	}
	
	/** 
	 * @param answer the answer to add
	 */
	public void add(Answer answer)
	{
		this.answers.add(answer);
	}

	/**
	 * @return the answers
	 */
	public List<Answer> getAnswers() {
		return answers;
	}

	/**
	 * @param answers the answers to set
	 */
	public void setAnswers(List<Answer> answers) {
		this.answers = answers;
	}

	
	/** 
	 * @return String
	 */
	@Override
	public String toString() {
		return "Answers [answers=" + answers + "]";
	}
	
}
