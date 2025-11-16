# ProAndroidDev Medium Article Crawler

A specialized crawler for fetching, summarizing, and organizing articles from the ProAndroidDev Medium publication into formats optimized for reading and LLM consumption.

## 🎯 Features

### Core Capabilities
- **Smart Crawling**: Crawls ProAndroidDev articles from multiple sources (archive, latest, RSS feed)
- **Date Filtering**: Filter articles by date (default: since August 2024)
- **Auto-Summarization**: Generates summaries, TL;DR, and key takeaways
- **Category Organization**: Automatically categorizes articles (Jetpack Compose, Kotlin, Architecture, etc.)
- **Multi-Format Export**:
  - 📄 **Markdown** for human reading with timeline and category navigation
  - 🤖 **JSONL** for direct LLM ingestion
  - 🔍 **Embeddings-ready** format for RAG systems

### Extracted Content
- Article title, subtitle, and full text
- Author information
- Publication date
- Reading time
- Claps and responses
- Tags and topics
- Code snippets
- Images

### Generated Content
- **Summary**: Concise overview of the article
- **TL;DR**: Quick reference with metadata
- **Key Takeaways**: Bullet points of main insights
- **Topics**: Extracted Android development topics
- **LLM Context**: Structured format for language models

## 📚 Categories

Articles are automatically categorized into:

1. **Jetpack Compose** - Declarative UI framework
2. **Kotlin** - Kotlin language and coroutines
3. **Architecture** - MVVM, MVI, Clean Architecture
4. **UI/UX** - Material Design, animations
5. **Testing** - Unit tests, UI tests
6. **Performance** - Optimization techniques
7. **Security** - Security best practices
8. **Libraries & Tools** - Third-party libraries, Gradle
9. **Best Practices** - Tips and patterns
10. **Tutorials** - How-to guides
11. **News & Updates** - Latest Android news
12. **Career** - Career development, interviews

## 🚀 Quick Start

### Using REST API

#### 1. Start Crawl
```bash
# Start the application
mvn spring-boot:run

# Start crawling
curl -X POST http://localhost:8080/api/medium-crawler/start
```

Response:
```json
{
  "status": "started",
  "message": "Crawl has been started...",
  "config": {
    "publication": "ProAndroidDev",
    "filterAfterDate": "2024-08-01T00:00:00",
    "maxArticles": 100,
    "outputDirectory": "./medium-articles-output"
  }
}
```

#### 2. Check Status
```bash
curl http://localhost:8080/api/medium-crawler/status
```

Response when completed:
```json
{
  "status": "completed",
  "message": "Crawl completed successfully",
  "stats": {
    "totalArticles": 45,
    "oldestArticle": "2024-08-15",
    "newestArticle": "2025-11-10",
    "totalClaps": 15234,
    "averageReadingTime": 7
  }
}
```

#### 3. View Configuration
```bash
curl http://localhost:8080/api/medium-crawler/config
```

#### 4. Update Date Filter
```bash
curl -X PUT http://localhost:8080/api/medium-crawler/config \
  -H "Content-Type: application/json" \
  -d '{"filterAfterDate": "2025-10-01T00:00:00"}'
```

### Programmatic Usage

```java
@Autowired
private MediumCrawlerService crawlerService;

public void crawlArticles() {
    CrawlResult result = crawlerService.executeCrawl();

    if (result.isSuccess()) {
        System.out.println("Found " + result.getArticles().size() + " articles");
        result.getArticles().forEach(article -> {
            System.out.println(article.getTitle());
            System.out.println(article.getSummary());
        });
    }
}
```

## ⚙️ Configuration

### Application Properties

```properties
# Publication Details
medium.crawler.publication-id=c72404660798
medium.crawler.publication-name=ProAndroidDev
medium.crawler.publication-url=https://proandroiddev.com

# Crawl Settings
medium.crawler.max-articles=100
medium.crawler.request-delay-ms=2000        # Polite crawling
medium.crawler.max-retries=3
medium.crawler.timeout-seconds=30

# Date Filtering
medium.crawler.filter-after-date=2024-08-01T00:00:00
medium.crawler.only-recent-articles=true

# Content Extraction
medium.crawler.extract-full-text=true
medium.crawler.extract-images=true
medium.crawler.extract-code-snippets=true

# Summarization
medium.crawler.generate-summaries=true
medium.crawler.generate-tldr=true
medium.crawler.generate-key-takeaways=true
medium.crawler.summary-max-length=500

# Output
medium.crawler.output-directory=./medium-articles-output
medium.crawler.generate-markdown=true
medium.crawler.generate-json=true
medium.crawler.generate-llm-format=true
```

### Customization Examples

**Crawl more articles:**
```properties
medium.crawler.max-articles=200
```

**Different date range:**
```properties
medium.crawler.filter-after-date=2025-01-01T00:00:00
```

**Custom output location:**
```properties
medium.crawler.output-directory=/path/to/custom/output
```

## 📂 Output Structure

```
medium-articles-output/
├── markdown/                           # Human-readable format
│   ├── README.md                      # Index with statistics
│   ├── timeline.md                    # Articles by month
│   ├── JETPACK_COMPOSE.md            # Category pages
│   ├── KOTLIN.md
│   └── articles/                      # Individual articles
│       ├── abc123.md
│       ├── def456.md
│       └── ...
└── llm-format/                        # LLM-optimized formats
    ├── proandroiddev-articles.jsonl   # All articles (JSONL)
    ├── embeddings-ready.jsonl         # Chunked for embeddings
    ├── metadata.json                  # Schema and statistics
    └── by-category/                   # Category-separated JSON
        ├── jetpack_compose.json
        ├── kotlin.json
        └── ...
```

## 📄 Output Formats

### Markdown Example

```markdown
# Jetpack Compose State Management Best Practices

| Property | Value |
|----------|-------|
| **Author** | John Doe |
| **Published** | October 15, 2024 |
| **Category** | Jetpack Compose |
| **Reading Time** | 8 min |

**Tags:** `Jetpack Compose` `State Management` `Best Practices`

## TL;DR

📱 Jetpack Compose State Management Best Practices
✍️ By John Doe
⏱️ 8 min read
📅 2024-10-15

Learn how to effectively manage state in Jetpack Compose...

## 🔑 Key Takeaways

1. Always hoist state to the appropriate level
2. Use remember for state that shouldn't survive recomposition
3. Prefer derivedStateOf for computed values
...
```

### JSONL Example

```json
{"id":"abc123","title":"Jetpack Compose State Management Best Practices","url":"https://proandroiddev.com/...","author":"John Doe","publishedDate":"2024-10-15","category":"Jetpack Compose","summary":"Learn effective state management in Jetpack Compose...","tldr":"📱 Jetpack Compose State Management Best Practices...","keyTakeaways":["Always hoist state...","Use remember..."],"topics":["Jetpack Compose","State Management"],"tags":["compose","state"],"readingTimeMinutes":8,"claps":234}
```

### Embeddings-Ready Example

```json
{"id":"abc123-overview","articleId":"abc123","chunkType":"overview","text":"Jetpack Compose State Management Best Practices by John Doe: Learn effective state management...","metadata":{"title":"...","author":"John Doe","category":"Jetpack Compose"}}
{"id":"abc123-tldr","articleId":"abc123","chunkType":"tldr","text":"📱 Jetpack Compose State Management Best Practices\n✍️ By John Doe...","metadata":{...}}
{"id":"abc123-takeaways","articleId":"abc123","chunkType":"key_takeaways","text":"Key takeaways: Always hoist state. Use remember...","metadata":{...}}
```

## 🔧 Using with LLMs

### 1. Direct Context Injection

```python
import json

# Load all articles
articles = []
with open('medium-articles-output/llm-format/proandroiddev-articles.jsonl') as f:
    for line in f:
        articles.append(json.loads(line))

# Use as context
context = "\n\n".join([a['llmContext'] for a in articles[:5]])
prompt = f"Context:\n{context}\n\nQuestion: What are the latest Jetpack Compose best practices?"
```

### 2. RAG with Vector Database

```python
from sentence_transformers import SentenceTransformer
import chromadb

# Load model
model = SentenceTransformer('all-MiniLM-L6-v2')

# Create collection
client = chromadb.Client()
collection = client.create_collection("proandroiddev_articles")

# Load embeddings-ready format
with open('medium-articles-output/llm-format/embeddings-ready.jsonl') as f:
    for line in f:
        chunk = json.loads(line)
        embedding = model.encode(chunk['text'])
        collection.add(
            embeddings=[embedding],
            documents=[chunk['text']],
            metadatas=[chunk['metadata']],
            ids=[chunk['id']]
        )

# Query
results = collection.query(
    query_texts=["How to manage state in Compose?"],
    n_results=5
)
```

### 3. Fine-tuning Dataset

```python
# Convert to instruction format
training_data = []
with open('medium-articles-output/llm-format/proandroiddev-articles.jsonl') as f:
    for line in f:
        article = json.loads(line)
        training_data.append({
            "instruction": f"Summarize this Android development article: {article['title']}",
            "input": article['fullText'],
            "output": article['summary']
        })

# Save for fine-tuning
with open('android_training.jsonl', 'w') as f:
    for item in training_data:
        f.write(json.dumps(item) + '\n')
```

### 4. Category-Specific Loading

```python
# Load only Jetpack Compose articles
with open('medium-articles-output/llm-format/by-category/jetpack_compose.json') as f:
    compose_articles = json.load(f)

# Use for specialized tasks
compose_context = "\n\n".join([a['summary'] for a in compose_articles])
```

## 📊 Example Results

After crawling ProAndroidDev since August 2024, you might see:

```
Total Articles: 45
Date Range: 2024-08-15 to 2025-11-10
Total Claps: 15,234
Average Reading Time: 7 minutes

Category Breakdown:
- Jetpack Compose: 15 articles
- Kotlin: 12 articles
- Architecture: 8 articles
- UI/UX: 6 articles
- Testing: 4 articles
```

## 🎓 Integration Examples

### With LangChain

```python
from langchain.document_loaders import JSONLoader
from langchain.vectorstores import FAISS
from langchain.embeddings import OpenAIEmbeddings

# Load documents
loader = JSONLoader(
    file_path='medium-articles-output/llm-format/proandroiddev-articles.jsonl',
    jq_schema='.llmContext',
    text_content=False
)
documents = loader.load()

# Create vector store
embeddings = OpenAIEmbeddings()
vectorstore = FAISS.from_documents(documents, embeddings)

# Query
retriever = vectorstore.as_retriever()
results = retriever.get_relevant_documents("Kotlin coroutines best practices")
```

### With LlamaIndex

```python
from llama_index import SimpleDirectoryReader, GPTVectorStoreIndex

# Load from category files
documents = SimpleDirectoryReader(
    'medium-articles-output/llm-format/by-category'
).load_data()

# Create index
index = GPTVectorStoreIndex.from_documents(documents)

# Query
response = index.query("What are the latest Android architecture patterns?")
print(response)
```

## 🔍 Troubleshooting

### No Articles Found

**Check network connectivity:**
```bash
curl -I https://proandroiddev.com
```

**Verify date filter:**
- Make sure `filter-after-date` isn't too recent
- Try setting to a few months ago

**Check logs:**
```bash
tail -f logs/spring.log | grep medium
```

### Rate Limiting (403 Errors)

**Increase delay between requests:**
```properties
medium.crawler.request-delay-ms=3000
```

**Reduce concurrent crawling:**
```properties
medium.crawler.max-articles=50
```

### Empty Summaries

**Check content extraction:**
```properties
medium.crawler.extract-full-text=true
```

**Increase summary length:**
```properties
medium.crawler.summary-max-length=800
```

## 💡 Tips & Best Practices

1. **Start with recent articles**: Use a recent `filter-after-date` to test
2. **Respect rate limits**: Keep `request-delay-ms` at 2000 or higher
3. **Monitor progress**: Use status endpoint during long crawls
4. **Category filtering**: Use category-specific JSON files for focused tasks
5. **Embeddings**: Use embeddings-ready format for better RAG performance
6. **Batch processing**: Process articles in batches if memory is limited

## 📈 Use Cases

✅ **Stay Updated**: Track latest Android development trends
✅ **Knowledge Base**: Build searchable Android article database
✅ **Learning Assistant**: Create AI assistant for Android Q&A
✅ **Content Analysis**: Analyze trending topics in Android development
✅ **Newsletter Generation**: Auto-generate Android newsletter content
✅ **Research**: Study evolution of Android development practices

## 🔗 Related Tools

- **Android Docs Crawler**: Crawl official Android documentation (see `ANDROID_DOCS_CRAWLER.md`)
- **Combined Use**: Use both crawlers for comprehensive Android knowledge base

## 📚 API Reference

### REST Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/medium-crawler/start` | POST | Start article crawl |
| `/api/medium-crawler/status` | GET | Check crawl status |
| `/api/medium-crawler/config` | GET | Get configuration |
| `/api/medium-crawler/config` | PUT | Update configuration |
| `/api/medium-crawler/stop` | POST | Stop running crawl |

### Java API

**Main Service**: `MediumCrawlerService`
- `executeCrawl()`: Execute complete pipeline
- `CrawlResult`: Results and statistics

**Crawler**: `MediumCrawler`
- `crawl()`: Crawl articles
- `getArticles()`: Get crawled articles

**Summarizer**: `ArticleSummarizer`
- `enhanceArticle(article)`: Add summaries and analysis

**Exporters**:
- `MediumMarkdownExporter.export()`: Generate Markdown
- `MediumLLMExporter.export()`: Generate LLM formats

## 🛡️ Responsible Crawling

- Default 2-second delay between requests
- Respects robots.txt
- Educational use only
- Includes proper user agent
- No aggressive scraping

## 📝 Output Schema

### LLM Document Fields

```typescript
{
  id: string;                    // Article ID
  title: string;                 // Article title
  url: string;                   // Medium URL
  author: string;                // Author name
  publishedDate: string;         // ISO date
  category: string;              // Auto-categorized
  summary: string;               // Generated summary
  tldr: string;                  // TL;DR version
  keyTakeaways: string[];        // Main points
  topics: string[];              // Extracted topics
  tags: string[];                // Medium tags
  fullText: string;              // Complete article
  readingTimeMinutes: number;    // Estimated time
  claps: number;                 // Article claps
  llmContext: string;            // Formatted for LLMs
}
```

---

**Happy Crawling! 📱📚**

For questions or issues, check the application logs or REST API status endpoint.
