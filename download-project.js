const https = require('https');
const fs = require('fs');

const url = 'https://start.spring.io/starter.zip?type=gradle-project-kotlin&language=kotlin&bootVersion=3.4.0&packaging=jar&jvmVersion=21&groupId=com.example&artifactId=bulletin-board&name=bulletin-board&description=Bulletin%20board%20project&packageName=com.example.bulletinboard&dependencies=web,data-jpa,h2,thymeleaf,devtools,validation,security';
const filePath = 'bulletin-board.zip';

https.get(url, (res) => {
    if (res.statusCode !== 200) {
        console.error(`Failed to download project. Status Code: ${res.statusCode}`);
        res.on('data', (d) => {
            process.stdout.write(d);
        });
        return;
    }

    const totalSize = parseInt(res.headers['content-length'], 10);
    let downloadedSize = 0;

    const fileStream = fs.createWriteStream(filePath);
    
    res.on('data', (chunk) => {
        downloadedSize += chunk.length;
        const percentage = ((downloadedSize / totalSize) * 100).toFixed(2);
        process.stdout.write(`Downloading: ${percentage}% (${downloadedSize} / ${totalSize} bytes)\r`);
    });

    res.pipe(fileStream);

    fileStream.on('finish', () => {
        fileStream.close();
        console.log('\nProject downloaded successfully to bulletin-board.zip');
    });

}).on('error', (err) => {
    console.error('Error during download:', err.message);
});