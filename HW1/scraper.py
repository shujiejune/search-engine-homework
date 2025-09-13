import requests
import json
import time
import re
from random import randint
from bs4 import BeautifulSoup
from bs4.element import Tag

# --- Configuration ---
# URL and selector specific to the Ask search engine
ASK_SEARCH_URL = "http://www.ask.com/web?q="

# User agent to mimic a real browser visit
USER_AGENT = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36'
}

# Input file containing 100 queries, one per line
QUERIES_FILE = "100QueriesSet3.txt"

# Output file for scraped results
OUTPUT_JSON_FILE = "hw1.json"

def scrape_ask_search(query: str) -> list:
    """
    Scrapes the Ask.com search results for a given query and returns the top 10 URLs.
    """
    search_url = ASK_SEARCH_URL + query.replace(" ", "+")
    results = []
    
    try:
        response = requests.get(search_url, headers=USER_AGENT)
        response.raise_for_status() # Raises an exception for bad status codes

        # Debug: save the HTML file
        with open('debug_ask_page.html', 'w', encoding='utf-8') as f:
            f.write(response.text)

        soup = BeautifulSoup(response.text, 'html.parser')
        
        # Find all script tags
        scripts = soup.find_all('script')

        # The data is stored in a script tag as a JavaScript variable assignment.
        # We need to find the one that defines 'window.MESON.initialState'.
        for script in scripts:
            # Check if the script contains the data object we need
            if isinstance(script, Tag) and script.string and 'window.MESON.initialState' in script.string:
                
                # Use regex to extract the JSON object from the script text
                # It looks for 'window.MESON.initialState = ' followed by a JSON object {}
                match = re.search(r'window\.MESON\.initialState\s*=\s*(\{.*\});', script.string)
                
                if match:
                    json_str = match.group(1)
                    
                    # Parse the extracted string as JSON
                    data = json.loads(json_str)
                    
                    # Navigate the nested dictionary to find the list of web results
                    # Use .get() to prevent errors if a key is missing
                    web_results = data.get('search', {}).get('webResults', {}).get('results', [])
                    
                    for result_item in web_results:
                        # Extract the URL from each result item
                        url = result_item.get('url')
                        if url:
                            results.append(url)
                        
                        # Stop once we have 10 results
                        if len(results) >= 10:
                            break
                    
                    # Once we've found and processed the data, we can exit the loop
                    break 
    except requests.exceptions.RequestException as e:
        print(f"  > Network error scraping for query '{query}': {e}")
        # If a query fails, return an empty list or handle it as needed.
        return []
    except json.JSONDecodeError:
        print(f"  > Failed to parse JSON data for query '{query}'.")
        return []
    except Exception as e:
        print(f"  > An unexpected error occurred for query '{query}': {e}")
        return []

    return results

def main():
    """
    Main function to read queries, scrape results, and save to a JSON file.
    """
    scraped_data = {}
    
    # Read queries from the input file
    try:
        with open(QUERIES_FILE, 'r') as f:
            queries = [line.strip() for line in f if line.strip()]
    except FileNotFoundError:
        print(f"Error: The query file '{QUERIES_FILE}' was not found.")
        return

    print(f"Starting to scrape {len(queries)} queries from Ask.com...")

    for i, query in enumerate(queries):
        print(f"Scraping query {i+1}/{len(queries)}: '{query}'")
        
        results = scrape_ask_search(query)
        scraped_data[query] = results
        
        # IMPORTANT: Wait with a random delay to avoid being blocked
        # The assignment specifies a random delay between 10 and 100 seconds.
        delay = randint(10, 100)
        print(f"  > Found {len(results)} results. Waiting for {delay} seconds...")
        time.sleep(delay)

    # Save the final dictionary to a JSON file
    with open(OUTPUT_JSON_FILE, 'w') as f:
        json.dump(scraped_data, f, indent=4)

    print(f"\nScraping complete. Results saved to '{OUTPUT_JSON_FILE}'.")


if __name__ == '__main__':
    main()
