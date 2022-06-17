The files included in this folder represent the json used for the Query Annotations in
RecommendationElasticSearchRepository, to be used for testing against a running ES instance.

At the time of writing, this query represents an attempt at a 1:1 match for the current
MongoDb query.

Note: Size, From and Sort should be omitted when adding the json to a @Query annotation
(Use Spring Pageable instead). Only expressions after "query": are needed in the annotation.

---
## findAllUnderNotice.json

This should be read as

``` underNotice = "Yes"
AND
designatedBody matches any from parameter 1
AND
existsInGmc = true
AND
  (
    doctorFirstName = parameter 0 (including partial)
    OR
    doctorLastName = parameter 0 (including partial)
    OR
    gmcNumber = parameter 0 (including partial)
    OR
    programmeName = parameter 0 (including partial)
    OR
    return everything (i.e if no parameter provided)
  )
```

---
## findAll.json

This should be read as

```
gmcReferenceNumber NOT IN parameter 2
AND
designatedBody matches any from parameter 1
AND
existsInGmc = true
AND
  (
    doctorFirstName = parameter 0 (including partial)
    OR
    doctorLastName = parameter 0 (including partial)
    OR
    gmcNumber = parameter 0 (including partial)
    OR
    return everything (i.e if no parameter provided)
  )
```
