# Android Documentation Crawler - Quick Start Guide

## 🚀 5-Minute Setup

### Prerequisites
- Java 17+
- Maven 3.6+
- Internet connection
- 1GB free disk space

### Step 1: Build the Application

```bash
mvn clean install
```

### Step 2: Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### Step 3: Start Crawling

**Option A: Using curl**
```bash
curl -X POST http://localhost:8080/api/crawler/start
```

**Option B: Using browser**
Visit: `http://localhost:8080/swagger-ui.html`
- Navigate to "Android Documentation Crawler"
- Click "POST /api/crawler/start"
- Click "Try it out" and "Execute"

### Step 4: Monitor Progress

```bash
curl http://localhost:8080/api/crawler/status
```

### Step 5: View Results

Once completed, check:
```bash
ls -la android-docs-output/

# View summary
cat android-docs-output/CRAWL_SUMMARY.md

# Browse markdown docs
open android-docs-output/markdown/README.md

# Check LLM format
head android-docs-output/llm-format/android-docs.jsonl
```

## 📋 Common Tasks

### Crawl Specific Packages Only

Edit `src/main/resources/application.properties`:
```properties
android.crawler.target-packages=android.widget,androidx.compose.ui
```

Then restart and crawl.

### Change Output Directory

```bash
curl -X PUT http://localhost:8080/api/crawler/config \
  -H "Content-Type: application/json" \
  -d '{"outputDirectory": "./my-custom-output"}'
```

### Increase Crawl Speed (Not Recommended)

```properties
android.crawler.request-delay-ms=500
android.crawler.max-pages-per-category=200
```

⚠️ **Warning**: Faster crawling may trigger rate limiting

### Generate Only LLM Format (Skip Markdown)

```properties
android.crawler.generate-markdown=false
android.crawler.generate-llm-format=true
```

## 🎯 Using the Output

### For Local LLM (Ollama, LM Studio, etc.)

1. **Load as context:**
```bash
# Get specific category
cat android-docs-output/llm-format/by-category/ui_components.json | \
  jq -r '.[].content' | \
  ollama run llama2
```

2. **Create embeddings:**
```python
# Use embeddings-ready.jsonl
import json
from sentence_transformers import SentenceTransformer

model = SentenceTransformer('all-MiniLM-L6-v2')

with open('android-docs-output/llm-format/embeddings-ready.jsonl') as f:
    for line in f:
        chunk = json.loads(line)
        embedding = model.encode(chunk['text'])
        # Store in your vector DB
```

### For RAG Systems

```python
# LangChain example
from langchain.document_loaders import JSONLoader
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain.vectorstores import Chroma

# Load documents
loader = JSONLoader(
    file_path='android-docs-output/llm-format/android-docs.jsonl',
    jq_schema='.content',
    text_content=False
)
docs = loader.load()

# Create vector store
vectorstore = Chroma.from_documents(docs, embedding_function)

# Query
results = vectorstore.similarity_search("How do I create a button?")
```

### For Fine-tuning

```python
# Convert to training format
import json

training_data = []
with open('android-docs-output/llm-format/android-docs.jsonl') as f:
    for line in f:
        doc = json.loads(line)
        training_data.append({
            "instruction": f"Explain {doc['name']} in Android",
            "input": "",
            "output": doc['content']
        })

# Save in your fine-tuning format
with open('android_training.jsonl', 'w') as f:
    for item in training_data:
        f.write(json.dumps(item) + '\n')
```

## 🔧 Troubleshooting

### "Crawl returns no data"

**Check configuration:**
```bash
curl http://localhost:8080/api/crawler/config
```

**Verify network:**
```bash
curl -I https://developer.android.com/reference
```

**Check logs:**
```bash
tail -f logs/spring.log | grep crawler
```

### "403 Forbidden errors"

**Increase delay:**
```properties
android.crawler.request-delay-ms=2000
```

**Or enable Selenium:**
```properties
android.crawler.use-selenium=true
```

### "Out of memory"

**Reduce scope:**
```properties
android.crawler.max-pages-per-category=50
android.crawler.max-depth=2
```

## 📊 Expected Results

After a successful crawl, you should see:

```
android-docs-output/
├── CRAWL_SUMMARY.md           # Summary report
├── markdown/
│   ├── README.md              # Index with charts (50+ KB)
│   ├── UI_COMPONENTS.md       # Category pages
│   ├── NETWORKING.md
│   └── UI_COMPONENTS/         # ~100+ API pages
│       ├── TextView.md
│       ├── Button.md
│       └── ...
└── llm-format/
    ├── android-docs.jsonl     # ~10-50 MB
    ├── embeddings-ready.jsonl # ~20-100 MB
    ├── metadata.json
    └── by-category/           # ~22 JSON files
        ├── ui_components.json
        └── ...
```

**Statistics:**
- Total APIs: 500-2000+ (depending on configuration)
- Categories: 22
- Processing time: 30-60 minutes
- Output size: 50-150 MB

## 🎓 Next Steps

1. **Explore the markdown documentation**
   ```bash
   cd android-docs-output/markdown
   # Open README.md in your markdown viewer
   ```

2. **Integrate with your LLM**
   - See examples above for Ollama, LangChain, LlamaIndex
   - Check ANDROID_DOCS_CRAWLER.md for more integration examples

3. **Customize the crawler**
   - Modify `AndroidCategory` enum for custom categories
   - Extend `ContentGenerator` for custom explanations
   - Create custom exporters for your specific format

4. **Schedule regular updates**
   ```bash
   # Add to crontab for weekly updates
   0 0 * * 0 curl -X POST http://localhost:8080/api/crawler/start
   ```

## 💡 Pro Tips

1. **Test with small scope first:**
   ```properties
   android.crawler.target-packages=android.widget
   android.crawler.max-pages-per-category=10
   ```

2. **Use category-specific files for faster loading:**
   ```python
   # Instead of loading all docs
   with open('llm-format/by-category/ui_components.json') as f:
       ui_docs = json.load(f)
   ```

3. **Combine with official Android samples:**
   - Cross-reference with github.com/android/architecture-samples
   - Augment with real-world code examples

4. **Create embeddings in batches:**
   ```python
   # Process in chunks to avoid memory issues
   batch_size = 100
   for i in range(0, len(chunks), batch_size):
       batch = chunks[i:i+batch_size]
       # Process batch
   ```

## 📚 Resources

- **Full Documentation**: See `ANDROID_DOCS_CRAWLER.md`
- **API Reference**: `http://localhost:8080/swagger-ui.html`
- **Configuration Reference**: See `application.properties`
- **Source Code**: `src/main/java/com/locationapp/service/crawler/`

---

**Ready to crawl! 🚀**

Got questions? Check the full documentation in `ANDROID_DOCS_CRAWLER.md`
