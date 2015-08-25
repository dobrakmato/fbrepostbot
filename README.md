fbrepostbot
-------------------
This application copies content from facebook pages to facebook pages (currently only that).

# How to use?

## Obtain jar.

Build it or download jar from build server.

> mvn package

## Configure main.conf.

After downloading put main.conf to folder with jar and configure it.

```
// Path to data folder.
dataPath = ./data/
// Path to fodler exposed by webserver.
publicPath = ./public/
// URL of exposed public folder.
publicPathUrl = http://google.com
```

## Configure access.conf.

Then put access.conf to your data folder (specified in main.conf).

```
// Facebook access token.
accessToken = YOURCOOLACCESSTOKEN
```

## Create rules for reposting / configure repost.conf.

### Reposting
Then create repost.conf file in your data folder.

Each line in file represents mapping in format `{source-page-id} -> {target-page-id}`.

You can find id of page by accessing <https://graph.facebook.com/{page-name}>.

### Crawling only

If you do not want to repost content from page, only crawl it, use `NO_TARGET` as target page id.

### Example repost.conf

```
1234567890000 -> 1515151515155
1212128585858 -> 1515151515155
8599211188821 -> NO_TARGET
```

## Summary

Your folder structure should now look like this:

    data
        access.conf
        repost.conf
    main.conf
    fbrepost.jar

## Final step

Run jar with java!

`java -jar fbrepost.jar`

----------------------

# Future steps

- Make use of Facebook subscription API. It provides nicer way to get nofitied about new posts then periodic checking.

# Known bugs

I know, there are bugs in this application. If you are kind, you can patch them a make pull request.