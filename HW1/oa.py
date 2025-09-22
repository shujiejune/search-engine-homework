import requests
from bs4 import BeautifulSoup
from bs4.element import Tag

def print_grid_from_gdoc_url(url: str):
    """
    Fetches data from a published Google Doc URL, parses the table,
    and prints a grid of characters based on the x and y coordinates.

    Args:
        url: The "Publish to the web" URL of the Google Doc.
    """
    print(f"Fetching data from: {url}")

    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
    }

    try:
        # 1. Fetch the HTML content of the page
        response = requests.get(url, headers=headers)
        response.raise_for_status()  # Raise an exception for bad status codes

        # 2. Parse the HTML to find the table data
        soup = BeautifulSoup(response.text, 'html.parser')

        content_div = soup.find('div', id='contents')
        if not isinstance(content_div, Tag):
            print("Error: Could not find the main content div with id='contents'. The page structure may have changed.")
            return

        table = content_div.find('table')
        if not isinstance(table, Tag):
            print("Error: Could not find a table tag in the Google Doc.")
            return

        points = {}
        # Find all rows in the table body
        rows = table.find_all('tr')

        # 3. Extract x and y coordinates from each row
        print("Parsing table data...")
        for row in rows:
            if isinstance(row, Tag):
                cells = row.find_all('td')
                # Ensure the row has the expected number of columns (3)
                if len(cells) == 3:
                    try:
                        # x is in the 1st column (index 0), y is in the 3rd (index 2)
                        x = int(cells[0].get_text(strip=True))
                        char = cells[1].get_text(strip=True)
                        y = int(cells[2].get_text(strip=True))
                        if not char:
                            char = '■'
                        points[(x, y)] = char
                    except (ValueError, IndexError):
                        # Skip the header row ('x-coordinate', etc.) and any malformed rows
                        continue
        
        if not points:
            print("No valid coordinate data was found in the table.")
            return

        print(f"Found {len(points)} data points. Building grid...")

        # 4. Determine the dimensions of the grid
        max_x = max(k[0] for k in points.keys())
        max_y = max(k[1] for k in points.keys())

        print("\n--- Character Grid ---")
        
        # 5. Build and print the grid
        # Loop y from its max value down to 0 to print from the top down.
        # This makes the y-axis appear to go from bottom-to-top in the output.
        for y in range(max_y, -1, -1):
            row_string = ""
            # Loop x from 0 to its max value for a left-to-right axis.
            for x in range(max_x + 1):
                # Get the character for the current (x, y) coordinate.
                # If the coordinate is not in our data, use a space ' '.
                character_to_print = points.get((x, y), ' ')
                row_string += character_to_print + " " # Add a space for better readability
            
            print(row_string)
        
        print("--- End of Grid ---\n")

    except requests.exceptions.RequestException as e:
        print(f"Error fetching the URL: {e}")
    except Exception as e:
        print(f"An unexpected error occurred: {e}")

if __name__ == '__main__':
    published_gdoc_url = "https://docs.google.com/document/d/e/2PACX-1vRPzbNQcx5UriHSbZ-9vmsTow_R6RRe7eyAU60xIF9Dlz-vaHiHNO2TKgDi7jy4ZpTpNqM7EvEcfr_p/pub"

    print_grid_from_gdoc_url(published_gdoc_url)
