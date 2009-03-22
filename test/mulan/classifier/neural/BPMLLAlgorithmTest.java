package mulan.classifier.neural;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BPMLLAlgorithmTest {

	private static final NeuralNet NEURAL_NET = new BasicNeuralNet(new int[] { 2, 10, 3 }, 1, ActivationTANH.class);
	private static final double LEARNING_RATE = 0.05;
	private static final double WEIGHTS_DECAY_COST = 0.00001;
	private static final double WRONG_WEIGHTS_DECAY_LOW = 0;
	private static final double WRONG_WEIGHTS_DECAY_HIGH = 1;
	private static final double[] INPUT_PATTERN = new double[] { -1, 1 };
	private static final double[] EXPECTED_LABELS = new double[] { -1, 1, -1 };
	private BPMLLAlgorithm algorithm;
	
	
	@Before
	public void setUp(){
		algorithm = new BPMLLAlgorithm(NEURAL_NET, WEIGHTS_DECAY_COST);
		
	}
	
	@After
	public void tearDown(){
		algorithm = null;
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testConstructorWithNullNeuralNet(){
		new BPMLLAlgorithm(null, WEIGHTS_DECAY_COST);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testConstructorWithWrongWeightsDecayLow(){
		new BPMLLAlgorithm(null, WRONG_WEIGHTS_DECAY_LOW);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testConstructorWithWrongWeightsDecayHigh(){
		new BPMLLAlgorithm(null, WRONG_WEIGHTS_DECAY_HIGH);
	}
	
	@Test
	public void testGetNetwork(){
		assertSame("Network model returend by the algorithm is not as expected.", 
				NEURAL_NET, algorithm.getNetwork());
	}
	
	@Test
	public void testGetWeightsDecayCost(){
		assertEquals("Weights decay cost term returend by the algorithm is not as expected.", 
				WEIGHTS_DECAY_COST, algorithm.getWeightsDecayCost(), 0);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testLearnWithNullInput(){
		algorithm.learn(null, EXPECTED_LABELS, LEARNING_RATE);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testLearnWithNullExpectedLabels(){
		algorithm.learn(INPUT_PATTERN, null, LEARNING_RATE);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testLearnWithWrongInput(){
		algorithm.learn(new double[0], EXPECTED_LABELS, LEARNING_RATE);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testLearnWithWrongExpectedLabels(){
		algorithm.learn(INPUT_PATTERN, new double[0], LEARNING_RATE);
	}
	
	@Test
	public void testGetNetworkError(){
		assertEquals("Learning errors should be same unless another learn iteration is performed.",
				algorithm.getNetworkError(INPUT_PATTERN, EXPECTED_LABELS),
				algorithm.getNetworkError(INPUT_PATTERN, EXPECTED_LABELS), 0);
	}
	
	@Test
	public void testAlgorithmLearnCore(){
		double error = algorithm.learn(INPUT_PATTERN, EXPECTED_LABELS, LEARNING_RATE);
		assertFalse("Learning error of the algorithm should not be zero.", error == 0);
		assertTrue("Returned network error should be smaller after updating the model by the algorithm.",
				error > algorithm.getNetworkError(INPUT_PATTERN, EXPECTED_LABELS));
		assertTrue("The error of the model should be decreasing with iterations.",
				error > algorithm.learn(INPUT_PATTERN, EXPECTED_LABELS, LEARNING_RATE));
	}
}