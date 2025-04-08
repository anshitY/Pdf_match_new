# 📄 PDF Comparison Service

A **Spring Boot** RESTful API for comparing multiple aspects of two PDF files. The service is designed to detect differences in **image and signature positions**, **raw text**, and **PDF structure/tags**, helping identify even subtle formatting discrepancies.

---

## 🔧 Features

- ✅ Compare embedded image positions across PDFs.
- ✅ Detect differences in visible or digital signature placement.
- ✅ Perform raw text comparison for textual differences.
- ✅ Analyze structural differences using PDF metadata/tags.
- ✅ Signature validation with configurable tolerance levels.
- ✅ Detect unique **text-image overlaps** (overshadowed text) not common between both PDFs.

---

## 📦 Endpoints

### 1. **Compare Image Positions**
**POST** `/image-positions/compare`  
Compares embedded images in both PDFs to detect positional or alignment differences.

---

### 2. **Compare Signature Positions**
**POST** `/signature/compare`  
Checks for visible or digital signatures and reports placement or presence differences.

---

### 3. **Compare Text Content**
**POST** `/compare_text/pdfs`  
Performs a raw text comparison to highlight any textual changes between PDFs.

---

### 4. **Compare PDF Tags/Structure**
**POST** `/compare/pdfs`  
Detects formatting or structural differences using PDF metadata and internal tagging.

---

### 5. **Signature Validation (with Tolerance)**
**POST** `/api/pdf/compare`  
Performs image and signature comparison using a configurable tolerance level to allow for minor deviations.

#### Parameters:
- `pdf1`: First PDF file (multipart/form-data)
- `pdf2`: Second PDF file (multipart/form-data)
- `tolerance`: *(Optional, default = 20)* — Maximum pixel threshold for considering two image/signature positions as matching.

---

### 6. **Compare Overshadowed Text**
**POST** `/api/pdf/compare-overshadowed`  
Detects **unique text elements that are overlapped by images** (i.e., overshadowed) in only one of the two PDFs.

#### Use Case:
To detect if a any text is covered by an image (such as a signature or stamp) in one PDF but not the other.

#### Parameters:
- `pdf1`: First PDF file (multipart/form-data)
- `pdf2`: Second PDF file (multipart/form-data)

---
### 7 **Compare PDF Formatting**
**POST** `/api/pdf/format/compare`  
Detects whether **two PDF files have identical or differing text formatting**, including font name, size, and position.

#### Use Case:
To verify if the visual **text formatting** (e.g., font styles, sizes, alignment) remains consistent across two versions of a PDF.

#### Parameters:
- `pdf1`: First PDF file (`multipart/form-data`)
- `pdf2`: Second PDF file (`multipart/form-data`)

#### Response:
- Returns a boolean:
  - `true` → formatting is the same (within defined tolerance)
  - `false` → formatting differs


## 📥 Request Format

All endpoints accept `multipart/form-data` with the following fields:

| Field | Type | Description |
|-------|------|-------------|
| `pdf1` | File | First PDF file |
| `pdf2` | File | Second PDF file |

For `/api/pdf/compare`, an additional optional field:

| Field | Type | Description |
|-------|------|-------------|
| `tolerance` | Integer | Pixel tolerance for comparing image/signature alignment (default: 20) |

---

## ✅ Sample CURL Requests

```bash
# Compare image positions
curl -X POST http://localhost:8080/image-positions/compare \
  -F "pdf1=@/path/to/first.pdf" \
  -F "pdf2=@/path/to/second.pdf"

# Compare signatures
curl -X POST http://localhost:8080/signature/compare \
  -F "pdf1=@/path/to/first.pdf" \
  -F "pdf2=@/path/to/second.pdf"

# Compare raw text
curl -X POST http://localhost:8080/compare_text/pdfs \
  -F "pdf1=@/path/to/first.pdf" \
  -F "pdf2=@/path/to/second.pdf"

# Compare PDF structure/tags
curl -X POST http://localhost:8080/compare/pdfs \
  -F "pdf1=@/path/to/first.pdf" \
  -F "pdf2=@/path/to/second.pdf"

# Image & signature comparison with tolerance
curl -X POST http://localhost:8080/api/pdf/compare \
  -F "pdf1=@/path/to/first.pdf" \
  -F "pdf2=@/path/to/second.pdf" \
  -F "tolerance=25"

# Compare unique overshadowed text
curl -X POST http://localhost:8080/api/pdf/compare-overshadowed \
  -F "pdf1=@/path/to/first.pdf" \
  -F "pdf2=@/path/to/second.pdf"
