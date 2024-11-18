package de.tostsoft.solarmonitoring.proxy.repository;

import de.tostsoft.solarmonitoring.proxy.model.ProxySolarSample;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProxySolarSampleRepository extends MongoRepository<ProxySolarSample,String> {


    @Aggregation({
            "{$match: { \"sample.systemId\": ?0} }",
            "{$limit: ?1 }"
    })
    public List<ProxySolarSample> findSomeSamplesWithSystemId(String systemId,int amount);

    @Aggregation(pipeline = {"{ $group : { _id : \"$sample.systemId\" , count: { $sum: 1 } }}"})
    public List<SampleSum> getSampleSums();

    public class SampleSum{
        public String id;
        public Long count;

        @Override
        public String toString() {
            return "SampleSum{" +
                    "id='" + id + '\'' +
                    ", count=" + count +
                    '}';
        }
    }
}
