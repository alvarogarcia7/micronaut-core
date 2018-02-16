/*
 * Copyright 2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.particleframework.http.client.aop

import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import org.particleframework.context.ApplicationContext
import org.particleframework.core.async.publisher.Publishers
import org.particleframework.http.annotation.Controller
import org.particleframework.http.annotation.Delete
import org.particleframework.http.annotation.Get
import org.particleframework.http.annotation.Patch
import org.particleframework.http.annotation.Post
import org.particleframework.http.client.Client
import org.particleframework.runtime.server.EmbeddedServer
import org.reactivestreams.Publisher
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicLong

/**
 * @author graemerocher
 * @since 1.0
 */
class RxJavaCrudSpec extends Specification {

    @Shared @AutoCleanup ApplicationContext context = ApplicationContext.run()
    @Shared EmbeddedServer embeddedServer = context.getBean(EmbeddedServer).start()

    void "test it is possible to implement CRUD operations with RxJava"() {
        given:
        BookClient client = context.getBean(BookClient)

        when:
        Book book = client.get(99)
                          .blockingGet()
        List<Book> books = client.list().blockingGet()

        then:
        book == null
        books.size() == 0

        when:
        book = client.save("The Stand").blockingGet()

        then:
        book != null
        book.title == "The Stand"
        book.id == 1

        when:
        book = client.get(book.id).blockingGet()

        then:
        book != null
        book.title == "The Stand"
        book.id == 1

        when:
        book = client.update(book.id, "The Shining").blockingGet()

        then:
        book != null
        book.title == "The Shining"
        book.id == 1

        when:
        book = client.delete(book.id).blockingGet()

        then:
        book != null

        when:
        book = client.get(book.id)
                .blockingGet()
        then:
        book == null
    }


    @Client('/rxjava/books')
    static interface BookClient extends BookApi {
    }

    @Controller("/rxjava/books")
    @Singleton
    static class BookController implements BookApi {

        Map<Long, Book> books = new LinkedHashMap<>()
        AtomicLong currentId = new AtomicLong(0)

        @Override
        Maybe<Book> get(Long id) {
            Book book = books.get(id)
            if(book)
                return Maybe.just(book)
            Maybe.empty()
        }

        @Override
        Single<List<Book>> list() {
            return Single.just(books.values().toList())
        }

        @Override
        Maybe<Book> delete(Long id) {
            Book book = books.remove(id)
            if(book) {
                return Maybe.just(book)
            }
            return Maybe.empty()
        }

        @Override
        Single<Book> save(String title) {
            Book book = new Book(title: title, id:currentId.incrementAndGet())
            books[book.id] = book
            return Single.just(book)
        }

        @Override
        Maybe<Book> update(Long id, String title) {
            Book book = books[id]
            if(book != null) {
                book.title = title
                return Maybe.just(book)
            }
            else {
                return Maybe.empty()
            }
        }
    }

    static interface BookApi {

        @Get("/{id}")
        Maybe<Book> get(Long id)

        @Get('/')
        Single<List<Book>> list()

        @Delete("/{id}")
        Maybe<Book> delete(Long id)

        @Post('/')
        Single<Book> save(String title)

        @Patch("/{id}")
        Maybe<Book> update(Long id, String title)
    }


    static class Book {
        Long id
        String title
    }
}
