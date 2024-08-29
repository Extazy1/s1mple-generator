<html>
    <head>
        <title>Welcome!</title>
    </head>
    <body>
    <h1>
        Welcome ${user}<#if user == "Big Joe">, our beloved leader</#if>!
    </h1>
    <p>Our latest product:
        <ul>
        <#list latestProducts as item>
            <li><a href="${item.url}">${item.name}</a>!</li>
        </#list>
        </ul>
    </body>
    <footer>
        ${currentYear} All rights reserved.
    </footer>
</html>