/**
 * Facebook Re-post Bot - Bot that checks some pages and copies posts to other pages.
 * Copyright (c) 2015, Matej Kormuth <http://www.github.com/dobrakmato>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package eu.matejkormuth.fbrepostbot;

import com.google.common.base.Charsets;
import eu.matejkormuth.fbrepostbot.facebook.FacebookPage;
import eu.matejkormuth.fbrepostbot.facebook.FacebookPost;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

public class PageCache {

    private final FacebookPage page;
    private final CachePathHelper pathHelper;
    private final Function<FacebookPost, CachedPost> transformer =
            facebookPost -> new CachedPost(facebookPost, null, null, false);

    public PageCache(FacebookPage page, PathHelper pathHelper) {
        this.page = page;
        this.pathHelper = new CachePathHelper(pathHelper.getPageCachePath(page.getId()));
    }

    public boolean contains(FacebookPost post) {
        return contains(post.getId());
    }

    public boolean contains(String postId) {
        return Files.exists(pathHelper.getPostPath(postId));
    }

    // Called from SourcePage
    public CachedPost add(FacebookPost post, SourcePage sourcePage) throws IOException {
        // Create cached post from facebook post.
        CachedPost cachedPost = transformer.apply(post);
        // Set source page of this post.
        cachedPost.setSourcePage(sourcePage);

        // Save it to cache.
        String json = cachedPost.serialize();
        Files.createDirectories(pathHelper.getPostPath(post.getId()).getParent());
        Files.write(pathHelper.getPostPath(post.getId()), json.getBytes(Charsets.UTF_8));

        // Return CachedPost object.
        return cachedPost;
    }

    // Called from TargetPage
    public CachedPost add(FacebookPost post, TargetPage targetPage) throws IOException {
        // Create cached post from facebook post.
        CachedPost cachedPost = transformer.apply(post);
        // Set source page of this post.
        cachedPost.setTargetPage(targetPage);

        // Save it to cache.
        String json = cachedPost.serialize();
        Files.createDirectories(pathHelper.getPostPath(post.getId()).getParent());
        Files.write(pathHelper.getPostPath(post.getId()), json.getBytes(Charsets.UTF_8));

        // Return CachedPost object.
        return cachedPost;
    }

    public CachePathHelper getPathHelper() {
        return pathHelper;
    }

    public static class CachePathHelper {
        private final Path basePath;

        public CachePathHelper(Path pageCachePath) {
            this.basePath = pageCachePath;
        }

        public Path getPath(String first, String... more) {
            return this.basePath.resolve(Paths.get(first, more));
        }

        public Path getPostPath(String postId) {
            return getPath("posts", postId + ".json");
        }
    }
}
