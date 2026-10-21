import Layout from "../components/Layout";
import BackHome from "../components/BackHome";

// Map centred on Guntur, Andhra Pradesh. OpenStreetMap needs no API key.
const MAP_URL =
  "https://www.openstreetmap.org/export/embed.html?bbox=80.4065%2C16.2867%2C80.4665%2C16.3267&layer=mapnik&marker=16.3067%2C80.4365";

export default function Contact() {
  return (
    <Layout>
      <h1>Contact</h1>
      <section className="panel">
        <p><strong>Name:</strong> Syed Razak</p>
        <p>
          <strong>Email:</strong>{" "}
          <a className="link" href="mailto:syedrazak337@gmail.com">syedrazak337@gmail.com</a>
        </p>
        <p>
          <strong>LinkedIn:</strong>{" "}
          <a className="link" href="https://www.linkedin.com/in/syed-razak-43aa0a241/" target="_blank" rel="noreferrer">
            linkedin.com/in/syed-razak-43aa0a241
          </a>
        </p>
        <p><strong>Address:</strong> Anandapet, 7th Line, Guntur, Andhra Pradesh, India</p>
      </section>

      <section className="panel">
        <h2>Map</h2>
        <iframe
          title="Map of Guntur, Andhra Pradesh"
          src={MAP_URL}
          loading="lazy"
          referrerPolicy="no-referrer"
          style={{ width: "100%", height: "320px", border: 0 }}
        />
        <p className="muted">
          The map is provided by OpenStreetMap. Loading it sends your IP address to OpenStreetMap.
        </p>
      </section>
      <BackHome />
    </Layout>
  );
}