package meta;

import mulan.classifier.lazy.BRkNN;
import mulan.classifier.meta.RAkEL;

public class RAkELTest extends MultiLabelMetaLearnerTest {

	@Override
	public void setUp() throws Exception {
		learner = new RAkEL(new BRkNN(10));
	}

}