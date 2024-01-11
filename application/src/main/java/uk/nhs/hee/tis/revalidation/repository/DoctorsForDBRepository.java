/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.revalidation.repository;


import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.entity.UnderNotice;

@Repository
public interface DoctorsForDBRepository extends MongoRepository<DoctorsForDB, String> {

  //Get count for trainee doctors who are underNotice
  long countByUnderNoticeIn(final UnderNotice... underNotice);

  @Query(value = "{ '$and' : [{ '$or' : [{'doctorFirstName' : { '$regex' : ?0, '$options' : 'i'}}, "
      + "{ 'doctorLastName' : { '$regex' : ?0, '$options' : 'i'}}, { '_id' : { '$regex' : ?0, '$options' : 'i'}}]}, "
      + "{ 'designatedBodyCode' : { '$in' : ?1 }}, { 'underNotice' : { '$in' : ?2 }}, "
      + "{ 'existsInGmc' : true }]}")
  Page<DoctorsForDB> findByUnderNotice(final Pageable pageable, final String searchQuery,
      final List<String> dbcs, final UnderNotice... underNotice);

  @Query(value = "{ '$and' : [{ '$or' : [{'doctorFirstName' : { '$regex' : ?0, '$options' : 'i'}}, "
      + "{ 'doctorLastName' : { '$regex' : ?0, '$options' : 'i'}}, { '_id' : { '$regex' : ?0, '$options' : 'i'}}]}, "
      + "{ 'designatedBodyCode' : { '$in' : ?1 }}, { '_id' : { '$nin' : ?2 }}, "
      + "{ 'existsInGmc' : true }]}")
  Page<DoctorsForDB> findAll(final Pageable pageable, final String searchQuery,
      final List<String> dbcs, final List<String> hiddenGmcIds);

  List<DoctorsForDB> findByExistsInGmcIsFalse();

  List<DoctorsForDB> findByDesignatedBodyCode(final String dbc);

}
