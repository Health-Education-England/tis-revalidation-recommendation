package uk.nhs.hee.tis.revalidation.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.entity.RevalidationSummary;

public interface RevalidationSummaryRepository extends MongoRepository<DoctorsForDB, String> {

  @Aggregation(pipeline = {"""
      {$lookup: {
        from: 'recommendation',
        localField: '_id',
        foreignField: 'gmcNumber',
        pipeline: [
             { "$sort": { "gmcSubmissionDate": -1 } },
             { "$limit": 1 }
           ],
        as: 'latestRecommendation'
      }}""", """
        {$unwind: {
        path: "$latestRecommendation",
      }}"""})
  List<RevalidationSummary> findByDesignatedBodyCode(final String designatedBodyCode);

  @Aggregation(pipeline = {"""
      {$lookup: {
        from: 'recommendation',
        localField: '_id',
        foreignField: 'gmcNumber',
        pipeline: [
             { "$sort": { "gmcSubmissionDate": -1 } },
             { "$limit": 1 }
           ],
        as: 'latestRecommendation'
      }}""", """
        {$unwind: {
        path: "$latestRecommendation",
      }}"""})
  List<RevalidationSummary> findByDesignatedBodyCodeIsNull();
}
