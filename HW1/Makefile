# Makefile for the Search Engine Comparison Project

# Use python3 as the default interpreter.
PYTHON = python3

# --- File Names ---
SCRAPED_JSON = hw1.json
FINAL_CSV = hw1.csv
QUERIES_TXT = 100QueriesSet4.txt
GOOGLE_JSON = Google_Result4.json

# --- Main Targets ---

# The default 'all' target will create the final CSV file.
# Running 'make' or 'make all' will trigger this.
all: $(FINAL_CSV)

# A convenience target 'run' that does the same as 'all'.
run: all

# --- Rule to Generate the Final CSV ---
# This target depends on the JSON output from the scraper.
# If hw1.json doesn't exist or is older than the analyzer script,
# Make will first run the rule for hw1.json.
$(FINAL_CSV): $(SCRAPED_JSON) analyzer.py $(GOOGLE_JSON)
	@echo "Analyzing scraped data to generate $(FINAL_CSV)..."
	$(PYTHON) analyzer.py
	@echo "Analysis complete. Results saved to $(FINAL_CSV)."

# --- Rule to Generate the Scraped JSON ---
# This target runs the scraper script.
# It depends on the scraper script itself and the list of queries.
$(SCRAPED_JSON): scraper.py $(QUERIES_TXT)
	@echo "Running scraper to generate $(SCRAPED_JSON)..."
	@echo "This will take a long time due to the built-in delays. Please be patient."
	$(PYTHON) scraper.py

# --- Housekeeping Target ---

# The 'clean' target is for removing generated files.
# It's useful for starting a fresh run.
clean:
	@echo "Cleaning up generated files..."
	rm -f $(SCRAPED_JSON) $(FINAL_CSV)
	@echo "Cleanup complete."

# --- Phony Targets ---
# These targets are not actual files. Declaring them as .PHONY
# ensures 'make' will run them even if a file with that name exists.
.PHONY: all run clean
