package mulan.classifier.neural;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class BasicNeuralNetTest {

	private static final double BIAS = 1;
	private static final int[] NET_TOPOLOGY = new int[] { 2, 10, 3 };
	private static final double[] INPUT_PATTERN = new double[] { 1.0, -1.0 };
	private Class<ActivationTANH> ACT_FUNCT_CLASS = ActivationTANH.class;
	private Class<ActivationLinear> ACT_FUNCT_INPUT_LAYER_CLASS = ActivationLinear.class;
	private BasicNeuralNet neuralNet;
	
	@Before
	public void setUp(){
		neuralNet = new BasicNeuralNet(NET_TOPOLOGY, BIAS, ACT_FUNCT_CLASS);
	}
	
	@After
	public void tearDown(){
		neuralNet = null;
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testConstructorWithNullTolopogy(){
		new BasicNeuralNet(null, BIAS, ACT_FUNCT_INPUT_LAYER_CLASS);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testConstructorWithWrongTolology(){
		new BasicNeuralNet(new int[] {0}, BIAS, ACT_FUNCT_INPUT_LAYER_CLASS);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testConstructorWithNullActFunction(){
		new BasicNeuralNet(NET_TOPOLOGY, BIAS, null);
	}
	
	@Test
	public void testGetLayersCount(){
		assertEquals("Count of neural network layers is not as expected.", NET_TOPOLOGY.length , neuralNet.getLayersCount());
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void testGetLayerUnitsUnmodifiableAdd(){
		List<Neuron> layer = neuralNet.getLayerUnits(0);
		layer.add(null);
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void testGetLayerUnitsUnmodifiableRemove(){
		List<Neuron> layer = neuralNet.getLayerUnits(0);
		layer.remove(0);
	}
	
	@Test
	public void testGetLayerUnits(){
		for(int layerIndex = 0; layerIndex < NET_TOPOLOGY.length; layerIndex++){
			List<Neuron> layer = neuralNet.getLayerUnits(layerIndex);
			assertEquals("Count of units of a layer is not as expected.", 
					NET_TOPOLOGY[layerIndex], layer.size());
			
			for(Neuron neuron : layer){
				assertEquals("Neuron bias is not as expected.", BIAS, neuron.getBiasInput(), 0);
				if(layerIndex == 0){
					assertEquals("Activation function of a neuron is not as expected.", 
							ACT_FUNCT_INPUT_LAYER_CLASS, neuron.getActivationFunction().getClass());
				}
				else{
					assertEquals("Activation function of a neuron is not as expected.", 
							ACT_FUNCT_CLASS, neuron.getActivationFunction().getClass());
				}
			}
		}
	}
	
	@Test
	public void testGetOutput(){
		double[] zeroArray = new double[NET_TOPOLOGY[NET_TOPOLOGY.length - 1]];
		Arrays.fill(zeroArray, 0);
		
		assertTrue("Network output is not as expected.", Arrays.equals(zeroArray, neuralNet.getOutput()));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testFeedForwardWithNull(){
		neuralNet.feedForward(null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testFeedForwardWithWrongInput(){
		neuralNet.feedForward(new double[0]);
	}
	
	@Test
	public void testFeedForward(){
		double[] result = neuralNet.feedForward(INPUT_PATTERN);
		
		assertTrue("Network output is not as expected.", Arrays.equals(result, neuralNet.getOutput()));
		double sum = 0;
		for(double item : result){
			sum += item;
		}
		assertFalse("The output of the network after processing input shoud not be zero.", sum == 0);
		assertTrue("The output of the network should be same as in previous feedForward call.", Arrays.equals(result, neuralNet.feedForward(INPUT_PATTERN)));
	}
	
	@Test
	public void testReset(){
		testFeedForward();
		neuralNet.reset();
		testGetOutput();
	}
	
	@Test
	public void testGetNetInputSize(){
		assertEquals("The size of the network input is not as expected.", 
				NET_TOPOLOGY[0], neuralNet.getNetInputSize());
	}
	
	@Test
	public void testGetNetOutputSize(){
		assertEquals("The size of the network output is not as expected.", 
				NET_TOPOLOGY[NET_TOPOLOGY.length - 1], neuralNet.getNetOutputSize());
	}

}