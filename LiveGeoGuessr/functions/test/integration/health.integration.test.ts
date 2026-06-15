const PROJECT_ID =
  "demo-live-geoguessr";

const REGION =
  "us-central1";

describe("health", () => {
  it("returns backend health information", async () => {
    const response = await fetch(
      "http://127.0.0.1:5001/" +
      `${PROJECT_ID}/${REGION}/health`,
    );

    expect(response.status).toBe(200);

    await expect(
      response.json(),
    ).resolves.toEqual({
      status: "ok",
      app: "Live GeoGuessr",
      backend:
        "Firebase Cloud Functions",
    });
  });
});